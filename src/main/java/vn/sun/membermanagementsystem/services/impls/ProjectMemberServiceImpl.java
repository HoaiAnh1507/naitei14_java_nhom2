package vn.sun.membermanagementsystem.services.impls;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.dto.response.ProjectMemberDTO;
import vn.sun.membermanagementsystem.entities.*;
import vn.sun.membermanagementsystem.enums.MembershipStatus;
import vn.sun.membermanagementsystem.repositories.ProjectMemberRepository;
import vn.sun.membermanagementsystem.repositories.TeamMemberRepository;
import vn.sun.membermanagementsystem.repositories.UserRepository;
import vn.sun.membermanagementsystem.services.ProjectMemberService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepo;
    private final UserRepository userRepo;
    private final TeamMemberRepository teamMemberRepo;

    @Override
    @Transactional
    public void removeAllMembers(Project project) {
        List<ProjectMember> activeMembers = projectMemberRepo.findByProjectAndStatus(project, ProjectMember.MemberStatus.ACTIVE);

        if (activeMembers.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        activeMembers.forEach(pm -> {
            pm.setStatus(ProjectMember.MemberStatus.INACTIVE);
            pm.setLeftAt(now);
        });

        projectMemberRepo.saveAll(activeMembers);
    }

    @Override
    @Transactional
    public void syncMembers(Project project, List<Long> requestedMemberIds, Long leaderId, Team team) {
        List<ProjectMember> currentMembers = projectMemberRepo.findByProjectAndStatus(project, ProjectMember.MemberStatus.ACTIVE);
        Set<Long> currentUserIds = currentMembers.stream().map(pm -> pm.getUser().getId()).collect(Collectors.toSet());

        Set<Long> requested = (requestedMemberIds == null) ? new HashSet<>() : new HashSet<>(requestedMemberIds);

        if (leaderId != null) requested.remove(leaderId);

        for (Long userId : requested) {
            if (!currentUserIds.contains(userId)) {
                ensureUserIsActiveMember(project, userId, team);
            }
        }

        for (ProjectMember pm : currentMembers) {
            Long uid = pm.getUser().getId();
            boolean isLeader = (leaderId != null && uid.equals(leaderId));

            if (!requested.contains(uid) && !isLeader) {
                pm.setStatus(ProjectMember.MemberStatus.INACTIVE);
                pm.setLeftAt(LocalDateTime.now());
                projectMemberRepo.save(pm);
            }
        }
    }

    @Override
    @Transactional
    public void ensureUserIsActiveMember(Project project, Long userId, Team team) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found id: " + userId));
        ensureUserIsActiveMember(project, user, team);
    }

    @Override
    @Transactional
    public void ensureUserIsActiveMember(Project project, User user, Team team) {

        ProjectMember pm = projectMemberRepo.findByProjectAndUser(project, user).orElse(null);

        if (pm == null) {
            pm = new ProjectMember();
            pm.setProject(project);
            pm.setUser(user);
            pm.setJoinedAt(LocalDateTime.now());
            pm.setStatus(ProjectMember.MemberStatus.ACTIVE);
            projectMemberRepo.save(pm);
        } else if (pm.getStatus() != ProjectMember.MemberStatus.ACTIVE) {
            pm.setStatus(ProjectMember.MemberStatus.ACTIVE);
            pm.setJoinedAt(LocalDateTime.now());
            pm.setLeftAt(null);
            projectMemberRepo.save(pm);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectMemberDTO> getProjectMembers(Long projectId, Pageable pageable) {
        return projectMemberRepo.findByProjectId(projectId, pageable)
                .map(pm -> {
                    ProjectMemberDTO dto = new ProjectMemberDTO();
                    dto.setId(pm.getId());
                    dto.setUserId(pm.getUser().getId());
                    dto.setUserName(pm.getUser().getName());
                    dto.setUserEmail(pm.getUser().getEmail());
                    dto.setJoinedAt(pm.getJoinedAt());
                    dto.setLeftAt(pm.getLeftAt());
                    dto.setStatus(pm.getStatus().name());
                    return dto;
                });
    }
}