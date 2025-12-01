package vn.sun.membermanagementsystem.services.impls;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectMapper projectMapper;
    private final TeamService teamService;

    private final ProjectMemberService membershipService;
    private final ProjectLeadershipService leadershipService;

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
        project.setStatus(Project.ProjectStatus.PLANNING);

        project = projectRepo.save(project);

        handleProjectContext(project, request.getTeamId(), request.getLeaderId(), request.getMemberIds());

        return projectMapper.toDTO(project);
    }

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_PROJECT", entityType = "PROJECT", description = "Update project")
    public ProjectDTO updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        project.setName(request.getName());
        project.setAbbreviation(request.getAbbreviation());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        project = projectRepo.save(project);

        handleProjectContext(project, request.getTeamId(), request.getLeaderId(), request.getMemberIds());

        return projectMapper.toDTO(project);
    }

    private void handleProjectContext(Project project, Long teamId, Long leaderId, List<Long> memberIds) {
        Team oldTeam = project.getTeam();
        Team newTeam = teamId != null ? teamService.getRequiredTeam(teamId) : null;

        if (!Objects.equals(oldTeam, newTeam)) {
            if (oldTeam != null && newTeam == null) {
                leadershipService.endAllLeadership(project);
                membershipService.removeAllMembers(project);
                project.setTeam(null);
            } else {
                project.setTeam(newTeam);
            }
            project = projectRepo.save(project);
        }

        membershipService.syncMembers(project, memberIds, leaderId, newTeam);

        leadershipService.updateLeader(project, leaderId, newTeam);

        if (leaderId != null) {
            membershipService.ensureUserIsActiveMember(project, leaderId, newTeam);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UpdateProjectRequest getUpdateProjectRequest(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

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
}