package vn.sun.membermanagementsystem.services.impls;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.annotation.LogActivity;
import vn.sun.membermanagementsystem.dto.request.CreateProjectRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateProjectRequest;
import vn.sun.membermanagementsystem.dto.response.ProjectDTO;
import vn.sun.membermanagementsystem.dto.response.ProjectDetailDTO;
import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.mapper.ProjectMapper;
import vn.sun.membermanagementsystem.repositories.ProjectRepository;
import vn.sun.membermanagementsystem.services.ProjectLeadershipService;
import vn.sun.membermanagementsystem.services.ProjectMemberService;
import vn.sun.membermanagementsystem.services.ProjectService;
import vn.sun.membermanagementsystem.services.TeamService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectMapper projectMapper;
    private final TeamService teamService;

    private final ProjectMemberService membershipService;
    private final ProjectLeadershipService leadershipService;

    private Project.ProjectStatus calculateStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();

        if (endDate != null && !now.isBefore(endDate)) {
            return Project.ProjectStatus.COMPLETED;
        }
        if (!now.isBefore(startDate)) {
            return Project.ProjectStatus.ONGOING;
        }
        return Project.ProjectStatus.PLANNING;
    }

    @Override
    public Page<ProjectDTO> getAllProjects(Long teamId, Pageable pageable) {
        if (teamId != null) {
            Team team = teamService.getRequiredTeam(teamId);
            return projectRepo.findByTeam(team, pageable).map(projectMapper::toDTO);
        }
        return projectRepo.findAll(pageable).map(projectMapper::toDTO);
    }

    @Override
    public ProjectDetailDTO getProjectDetail(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        return projectMapper.toDetailDTO(project);
    }

    @Override
    @Transactional
    @LogActivity(action = "CREATE_PROJECT", entityType = "PROJECT", description = "Create new project")
    public ProjectDTO createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setAbbreviation(request.getAbbreviation());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(calculateStatus(request.getStartDate(), request.getEndDate()));

        if (request.getTeamId() != null) {
            Team team = teamService.getRequiredTeam(request.getTeamId());
            project.setTeam(team);
        }

        project = projectRepo.save(project);

        handleProjectContextMembersOnly(project, request.getLeaderId(), request.getMemberIds());

        return projectMapper.toDTO(project);
    }

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_PROJECT", entityType = "PROJECT", description = "Update project")
    public ProjectDTO updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        if (project.getStatus() == Project.ProjectStatus.COMPLETED ||
                project.getStatus() == Project.ProjectStatus.CANCELLED) {
            throw new IllegalStateException("Không thể chỉnh sửa dự án đã hoàn thành hoặc đã bị huỷ.");
        }

        project.setName(request.getName());
        project.setAbbreviation(request.getAbbreviation());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        Project.ProjectStatus newStatus = calculateStatus(request.getStartDate(), request.getEndDate());
        if (project.getStatus() != Project.ProjectStatus.CANCELLED) {
            project.setStatus(newStatus);
        }

        project = projectRepo.save(project);

        handleProjectContextMembersOnly(project, request.getLeaderId(), request.getMemberIds());
        return projectMapper.toDTO(project);
    }

    private void handleProjectContextMembersOnly(Project project, Long leaderId, List<Long> memberIds) {
        Team currentTeam = project.getTeam();

        membershipService.syncMembers(project, memberIds, leaderId, currentTeam);
        leadershipService.updateLeader(project, leaderId, currentTeam);

        if (leaderId != null) {
            membershipService.ensureUserIsActiveMember(project, leaderId, currentTeam);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UpdateProjectRequest getUpdateProjectRequest(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        if (project.getStatus() == Project.ProjectStatus.COMPLETED ||
                project.getStatus() == Project.ProjectStatus.CANCELLED) {
            throw new IllegalStateException("Dự án này đã kết thúc, không thể chỉnh sửa.");
        }

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setId(project.getId());
        req.setName(project.getName());
        req.setAbbreviation(project.getAbbreviation());
        req.setStartDate(project.getStartDate());
        req.setEndDate(project.getEndDate());

        if (project.getTeam() != null) {
            req.setTeamId(project.getTeam().getId());
        }

        project.getLeadershipHistory().stream()
                .filter(h -> h.getEndedAt() == null)
                .findFirst()
                .ifPresent(h -> req.setLeaderId(h.getLeader().getId()));


        List<Long> activeMemberIds = project.getProjectMembers().stream()
                .filter(pm -> pm.getStatus() == vn.sun.membermanagementsystem.entities.ProjectMember.MemberStatus.ACTIVE)
                .map(pm -> pm.getUser().getId())
                .collect(Collectors.toList());

        req.setMemberIds(activeMemberIds);

        return req;
    }

    @Override
    @Transactional
    @LogActivity(action = "CANCEL_PROJECT", entityType = "PROJECT", description = "Cancel project and deactivate members")
    public void cancelProject(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        if (project.getStatus() == Project.ProjectStatus.COMPLETED ||
                project.getStatus() == Project.ProjectStatus.CANCELLED) {
            throw new IllegalStateException("Không thể xoá dự án đã hoàn thành hoặc đã bị huỷ.");
        }

        project.setStatus(Project.ProjectStatus.CANCELLED);
        project.setDeletedAt(java.time.LocalDateTime.now());
        leadershipService.endAllLeadership(project);

        membershipService.removeAllMembers(project);

        projectRepo.save(project);
    }

    @Transactional
    @Override
    public void updateAllProjectStatuses() {
        List<Project> activeProjects = projectRepo.findAll().stream()
                .filter(p -> p.getStatus() != Project.ProjectStatus.CANCELLED)
                .toList();

        int count = 0;
        for (Project project : activeProjects) {
            Project.ProjectStatus newStatus = calculateStatus(project.getStartDate(), project.getEndDate());

            if (project.getStatus() != newStatus) {
                log.info("Auto-updating project ID {} from {} to {}", project.getId(), project.getStatus(), newStatus);
                project.setStatus(newStatus);
                projectRepo.save(project);
                count++;
            }
        }
        if (count > 0) {
            log.info("Scheduler updated status for {} projects.", count);
        }
    }
}