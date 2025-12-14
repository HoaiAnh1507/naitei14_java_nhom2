package vn.sun.membermanagementsystem.services.impls;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.annotation.LogActivity;
import vn.sun.membermanagementsystem.dto.request.CreateTeamRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateTeamRequest;
import vn.sun.membermanagementsystem.dto.response.TeamDTO;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.dto.response.TeamLeaderDTO;
import vn.sun.membermanagementsystem.dto.response.TeamStatisticsDTO;
import vn.sun.membermanagementsystem.dto.response.UserSelectionDTO;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.entities.TeamMember;
import vn.sun.membermanagementsystem.entities.User;
import vn.sun.membermanagementsystem.enums.MembershipStatus;
import vn.sun.membermanagementsystem.exception.BadRequestException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.mapper.TeamMapper;
import vn.sun.membermanagementsystem.exception.DuplicateResourceException;
import vn.sun.membermanagementsystem.repositories.TeamMemberRepository;
import vn.sun.membermanagementsystem.repositories.TeamRepository;
import vn.sun.membermanagementsystem.repositories.UserRepository;
import vn.sun.membermanagementsystem.services.TeamLeadershipService;
import vn.sun.membermanagementsystem.services.TeamService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TeamLeadershipService teamLeadershipService;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamServiceImpl(
            TeamRepository teamRepository,
            TeamMapper teamMapper,
            @Lazy TeamLeadershipService teamLeadershipService,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
        this.teamLeadershipService = teamLeadershipService;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<TeamDTO> getTeamById(Long id) {
        return teamRepository.findById(id)
                .map(teamMapper::toDTO);
    }

    @Override
    public Team getRequiredTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSelectionDTO> getActiveUsersByTeam(Long teamId) {
        log.debug("Loading active users for teamId={}", teamId);
        List<User> users = teamMemberRepository.findActiveUsersByTeamId(teamId);
        return users.stream()
                .map(u -> new UserSelectionDTO(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());
    }

    @Transactional
    @LogActivity(action = "CREATE_TEAM", entityType = "TEAM", description = "Create new team")
    public TeamDTO createTeam(CreateTeamRequest request) {
        log.info("Creating team with name: {}", request.getName());

        if (teamRepository.existsByNameAndNotDeleted(request.getName())) {
            log.error("Team name already exists: {}", request.getName());
            throw new DuplicateResourceException("Team name already exists: " + request.getName());
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());

        Team savedTeam = teamRepository.save(team);
        log.info("Team created successfully with ID: {}", savedTeam.getId());

        if (request.getLeaderId() != null) {
            log.info("Assigning leader {} to team {}", request.getLeaderId(), savedTeam.getId());
            teamLeadershipService.assignLeader(savedTeam.getId(), request.getLeaderId());
        }

        return teamMapper.toDTO(savedTeam);
    }

    @Transactional
    @LogActivity(action = "UPDATE_TEAM", entityType = "TEAM", description = "Update team information")
    public TeamDTO updateTeam(Long id, UpdateTeamRequest request) {
        log.info("Updating team with ID: {}", id);
        log.info("Request data - Name: {}, Description length: {}, LeaderId: {}",
                request.getName(),
                request.getDescription() != null ? request.getDescription().length() : 0,
                request.getLeaderId());

        Team team = teamRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", id);
                    return new ResourceNotFoundException("Team not found with ID: " + id);
                });

        if (request.getName() != null && !request.getName().equals(team.getName())) {
            if (teamRepository.existsByNameAndNotDeletedAndIdNot(request.getName(), id)) {
                log.error("Team name already exists: {}", request.getName());
                throw new DuplicateResourceException("Team name already exists: " + request.getName());
            }
            team.setName(request.getName());
        }

        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        if (request.getLeaderId() != null) {
            handleLeaderChange(id, request.getLeaderId());
        }

        team.setUpdatedAt(LocalDateTime.now());

        Team updatedTeam = teamRepository.save(team);
        log.info("Team updated successfully with ID: {}", updatedTeam.getId());

        return teamMapper.toDTO(updatedTeam);
    }

    @Transactional
    @LogActivity(action = "DELETE_TEAM", entityType = "TEAM", description = "Delete team")
    public boolean deleteTeam(Long id) {
        log.info("Soft deleting team with ID: {}", id);

        Team team = teamRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", id);
                    return new ResourceNotFoundException("Team not found with ID: " + id);
                });

        boolean hasActiveProjects = teamRepository.hasActiveProjects(id);
        if (hasActiveProjects) {
            log.error("Cannot delete team with ID {} - has active projects", id);
            throw new BadRequestException("Cannot delete team with active projects.");
        }

        TeamLeaderDTO currentLeader = teamLeadershipService.getCurrentLeader(id);
        if (currentLeader != null) {
            teamLeadershipService.removeLeader(id);
            log.info("Removed leader from team ID: {}", id);
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndLeftAtIsNull(id);
        LocalDateTime now = LocalDateTime.now();
        for (TeamMember member : activeMembers) {
            member.setLeftAt(now);
            member.setStatus(MembershipStatus.INACTIVE);
            teamMemberRepository.save(member);
            log.info("Removed member {} from team {}", member.getUser().getId(), id);
        }

        // Rename team with .deleted suffix before soft delete
        String originalName = team.getName();
        if (!originalName.endsWith(".deleted")) {
            team.setName(originalName + ".deleted");
        }

        // Perform soft delete
        team.setDeletedAt(LocalDateTime.now());
        teamRepository.save(team);

        log.info("Team soft deleted successfully with ID: {}, renamed to: {}", id, team.getName());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailDTO getTeamDetail(Long id) {
        log.info("Getting team detail with ID: {}", id);

        Team team = teamRepository.findByIdWithLeadershipHistory(id)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", id);
                    return new ResourceNotFoundException("Team not found with ID: " + id);
                });

        team.getTeamMemberships().forEach(tm -> tm.getUser().getName());
        team.getLeadershipHistory().forEach(lh -> lh.getLeader().getName());
        team.getProjects().forEach(p -> p.getName());

        TeamDetailDTO detailDTO = teamMapper.toDetailDTO(team);

        teamMapper.populateTeamDetailDTO(detailDTO, team);

        log.info("Team detail retrieved successfully for ID: {}", id);
        return detailDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeams() {
        log.info("Getting all teams");

        List<Team> teams = teamRepository.findAllNotDeleted();
        return teamMapper.toDTOList(teams);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamDTO> getAllTeamsWithPagination(Pageable pageable) {
        log.info("Getting all teams with pagination: page {}, size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Team> teamPage = teamRepository.findAllNotDeleted(pageable);

        return teamPage.map(team -> {
            TeamDTO dto = teamMapper.toDTO(team);

            team.getLeadershipHistory().stream()
                    .filter(lh -> lh.getEndedAt() == null)
                    .findFirst()
                    .ifPresent(lh -> {
                        TeamLeaderDTO leaderDTO = new TeamLeaderDTO();
                        leaderDTO.setUserId(lh.getLeader().getId());
                        leaderDTO.setName(lh.getLeader().getName());
                        leaderDTO.setStartedAt(lh.getStartedAt());
                        dto.setCurrentLeader(leaderDTO);
                    });

            long memberCount = teamRepository.countActiveMembers(team.getId());
            dto.setMemberCount((int) memberCount);

            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TeamStatisticsDTO getTeamStatistics(Long teamId) {
        log.info("Getting statistics for team ID: {}", teamId);

        Team team = teamRepository.findByIdAndNotDeleted(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", teamId);
                    return new ResourceNotFoundException("Team not found with ID: " + teamId);
                });

        TeamStatisticsDTO stats = new TeamStatisticsDTO();
        stats.setTeamId(team.getId());
        stats.setTeamName(team.getName());

        // Get all member statistics
        long activeMembersCount = teamRepository.countActiveMembers(teamId);
        stats.setActiveMembers((int) activeMembersCount);

        // Calculate total and inactive members from team memberships
        long totalMembersCount = team.getTeamMemberships().stream()
                .filter(tm -> tm.getLeftAt() == null)
                .count();
        stats.setTotalMembers((int) totalMembersCount);
        stats.setInactiveMembers((int) (totalMembersCount - activeMembersCount));

        // Get all project statistics
        long totalProjectsCount = teamRepository.countAllProjects(teamId);
        long activeProjectsCount = teamRepository.countActiveProjects(teamId);
        long completedProjectsCount = teamRepository.countCompletedProjects(teamId);

        stats.setTotalProjects((int) totalProjectsCount);
        stats.setActiveProjects((int) activeProjectsCount);
        stats.setCompletedProjects((int) completedProjectsCount);

        // Get current leader
        team.getLeadershipHistory().stream()
                .filter(lh -> lh.getEndedAt() == null)
                .findFirst()
                .ifPresent(lh -> {
                    TeamLeaderDTO leaderDTO = new TeamLeaderDTO();
                    leaderDTO.setUserId(lh.getLeader().getId());
                    leaderDTO.setName(lh.getLeader().getName());
                    leaderDTO.setStartedAt(lh.getStartedAt());
                    stats.setCurrentLeader(leaderDTO);
                });

        log.info("Team statistics retrieved successfully for ID: {}", teamId);
        return stats;
    }

    private void handleLeaderChange(Long teamId, Long newLeaderId) {
        log.info("Handling leader change for team {}", teamId);

        TeamLeaderDTO currentLeader = teamLeadershipService.getCurrentLeader(teamId);
        if (currentLeader != null && currentLeader.getUserId().equals(newLeaderId)) {
            log.info("New leader is same as current leader, skipping change");
            return;
        }

        try {
            teamLeadershipService.changeLeader(teamId, newLeaderId);
        } catch (BadRequestException e) {
            if (e.getMessage().contains("has no active leader")) {
                log.info("Team has no active leader, assigning new leader");
                teamLeadershipService.assignLeader(teamId, newLeaderId);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogActivity(action = "ADD_MEMBER_TO_TEAM", entityType = "TEAM", description = "Add member to team")
    public void addMemberToTeam(Long teamId, Long userId) {
        log.info("Adding user {} to team {}", userId, teamId);

        Team team = teamRepository.findByIdAndNotDeleted(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", teamId);
                    return new ResourceNotFoundException("Team not found with ID: " + teamId);
                });

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        TeamMember existingMembership = teamMemberRepository.findActiveTeamByUserId(userId);
        if (existingMembership != null) {
            if (existingMembership.getTeam().getId().equals(teamId)) {
                log.warn("User {} is already an active member of team {}", userId, teamId);
                throw new BadRequestException("User is already a member of this team");
            } else {
                log.error("User {} is already an active member of another team", userId);
                throw new BadRequestException(
                        "User is already a member of another team. They must leave that team first.");
            }
        }

        TeamMember newMembership = new TeamMember();
        newMembership.setUser(user);
        newMembership.setTeam(team);
        newMembership.setStatus(MembershipStatus.ACTIVE);
        newMembership.setJoinedAt(LocalDateTime.now());

        teamMemberRepository.save(newMembership);
        log.info("User {} successfully added to team {}", userId, teamId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogActivity(action = "ADD_MEMBERS_TO_TEAM", entityType = "TEAM", description = "Add multiple members to team")
    public int addMembersToTeam(Long teamId, List<Long> userIds) {
        log.info("Adding {} users to team {}", userIds.size(), teamId);

        Team team = teamRepository.findByIdAndNotDeleted(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", teamId);
                    return new ResourceNotFoundException("Team not found with ID: " + teamId);
                });

        int addedCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Long userId : userIds) {
            try {
                User user = userRepository.findByIdAndNotDeleted(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

                TeamMember existingMembership = teamMemberRepository.findActiveTeamByUserId(userId);
                if (existingMembership != null) {
                    if (existingMembership.getTeam().getId().equals(teamId)) {
                        log.warn("User {} is already an active member of team {}", userId, teamId);
                        errors.append(String.format("%s is already a member; ", user.getName()));
                        continue;
                    } else {
                        log.warn("User {} is already an active member of another team", userId);
                        errors.append(String.format("%s is in another team; ", user.getName()));
                        continue;
                    }
                }

                TeamMember newMembership = new TeamMember();
                newMembership.setUser(user);
                newMembership.setTeam(team);
                newMembership.setStatus(MembershipStatus.ACTIVE);
                newMembership.setJoinedAt(LocalDateTime.now());

                teamMemberRepository.save(newMembership);
                addedCount++;
                log.info("User {} successfully added to team {}", userId, teamId);

            } catch (ResourceNotFoundException e) {
                log.warn("User {} not found, skipping", userId);
                errors.append(String.format("User ID %d not found; ", userId));
            }
        }

        if (addedCount == 0 && !userIds.isEmpty()) {
            throw new BadRequestException("No users were added. " + errors.toString().trim());
        }

        log.info("Successfully added {} out of {} users to team {}", addedCount, userIds.size(), teamId);
        return addedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogActivity(action = "REMOVE_MEMBER_FROM_TEAM", entityType = "TEAM", description = "Remove member from team")
    public void removeMemberFromTeam(Long teamId, Long userId) {
        log.info("Removing user {} from team {}", userId, teamId);

        Team team = teamRepository.findByIdAndNotDeleted(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", teamId);
                    return new ResourceNotFoundException("Team not found with ID: " + teamId);
                });

        TeamMember membership = teamMemberRepository.findActiveTeamByUserId(userId);

        if (membership == null || !membership.getTeam().getId().equals(teamId)) {
            log.error("User {} is not an active member of team {}", userId, teamId);
            throw new BadRequestException("User is not a member of this team");
        }

        boolean isCurrentLeader = team.getLeadershipHistory().stream()
                .anyMatch(lh -> lh.getEndedAt() == null && lh.getLeader().getId().equals(userId));

        if (isCurrentLeader) {
            log.error("Cannot remove user {} because they are the current leader of team {}", userId, teamId);
            throw new BadRequestException("Cannot remove the current team leader. Please assign a new leader first.");
        }

        membership.setStatus(MembershipStatus.INACTIVE);
        membership.setLeftAt(LocalDateTime.now());

        teamMemberRepository.save(membership);
        log.info("User {} successfully removed from team {}", userId, teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamDetailDTO.TeamMemberDTO> getTeamMembersWithPagination(Long teamId, Pageable pageable) {
        log.info("Getting team members with pagination for team ID: {}", teamId);

        Team team = teamRepository.findByIdAndNotDeleted(teamId)
                .orElseThrow(() -> {
                    log.error("Team not found with ID: {}", teamId);
                    return new ResourceNotFoundException("Team not found with ID: " + teamId);
                });

        Page<TeamMember> memberPage = teamMemberRepository.findActiveTeamMembersByTeamId(teamId, pageable);

        return memberPage.map(tm -> {
            User user = tm.getUser();
            String positionName = null;

            if (user.getPositionHistories() != null && !user.getPositionHistories().isEmpty()) {
                positionName = user.getPositionHistories().stream()
                        .filter(ph -> ph.getEndedAt() == null && ph.getPosition() != null)
                        .findFirst()
                        .map(ph -> ph.getPosition().getName())
                        .orElse(null);
            }

            return TeamDetailDTO.TeamMemberDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .position(positionName)
                    .joinedAt(tm.getJoinedAt())
                    .build();
        });
    }
}
