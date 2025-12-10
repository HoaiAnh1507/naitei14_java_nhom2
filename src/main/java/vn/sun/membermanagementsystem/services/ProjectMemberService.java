package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.sun.membermanagementsystem.dto.response.ProjectMemberDTO;
import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.entities.User;

import java.util.List;

public interface ProjectMemberService {
    void removeAllMembers(Project project);
    void syncMembers(Project project, List<Long> requestedMemberIds, Long leaderId, Team team);
    void ensureUserIsActiveMember(Project project, Long userId, Team team);
    void ensureUserIsActiveMember(Project project, User user, Team team);
    Page<ProjectMemberDTO> getProjectMembers(Long projectId, Pageable pageable);
}