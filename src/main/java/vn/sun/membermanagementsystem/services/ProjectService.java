package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.annotation.LogActivity;
import vn.sun.membermanagementsystem.dto.request.CreateProjectRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateProjectRequest;
import vn.sun.membermanagementsystem.dto.response.ProjectDTO;
import vn.sun.membermanagementsystem.dto.response.ProjectDetailDTO;


public interface ProjectService {
    Page<ProjectDTO> getAllProjects(Long teamId, Pageable pageable);
    ProjectDetailDTO getProjectDetail(Long id);
    ProjectDTO createProject(CreateProjectRequest request);
    ProjectDTO updateProject(Long id, UpdateProjectRequest request);
    UpdateProjectRequest getUpdateProjectRequest(Long id);
    void cancelProject(Long id);
    void updateAllProjectStatuses();
}