package vn.sun.membermanagementsystem.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sun.membermanagementsystem.dto.request.CreateProjectRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateProjectRequest;
import vn.sun.membermanagementsystem.dto.response.*;
import vn.sun.membermanagementsystem.enums.UserStatus;
import vn.sun.membermanagementsystem.services.ProjectMemberService;
import vn.sun.membermanagementsystem.services.ProjectService;
import vn.sun.membermanagementsystem.services.TeamService;
import vn.sun.membermanagementsystem.services.UserService;

import java.util.List;

@Controller
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectService projectService;
    private final TeamService teamService;
    private final UserService userService;
    private final ProjectMemberService projectMemberService;

    private static final List<Integer> PAGE_SIZES = List.of(10, 25, 50, 100);

    @ModelAttribute("teams")
    public List<TeamDTO> populateTeams() {
        return teamService.getAllTeams();
    }

    @ModelAttribute("allUsers")
    public List<UserListItemDTO> populateActiveUsers() {
        return userService.getUsersByStatus(UserStatus.ACTIVE);
    }

    @ModelAttribute("pageSizeOptions")
    public List<Integer> populatePageSizeOptions() {
        return PAGE_SIZES;
    }

    @GetMapping
    public String listProjects(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long teamId
    ) {
        if (!PAGE_SIZES.contains(size)) size = 10;

        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<ProjectDTO> projectsPage = projectService.getAllProjects(teamId, pageable);

        model.addAttribute("projects", projectsPage);
        model.addAttribute("selectedTeamId", teamId);
        model.addAttribute("currentPageSize", size);

        return "admin/projects/index";
    }

    @GetMapping("/{id}")
    public String viewProjectDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joinedAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        if (!PAGE_SIZES.contains(size)) size = 10;

        ProjectDetailDTO project = projectService.getProjectDetail(id);
        model.addAttribute("project", project);

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ProjectMemberDTO> memberPage = projectMemberService.getProjectMembers(id, pageable);

        model.addAttribute("memberPage", memberPage);
        model.addAttribute("currentPageSize", size);

        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc"); // Để đảo chiều khi click

        return "admin/projects/detail";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("projectRequest")) {
            model.addAttribute("projectRequest", new CreateProjectRequest());
        }
        return "admin/projects/create";
    }

    @PostMapping("/create")
    public String createProject(
            @Valid @ModelAttribute("projectRequest") CreateProjectRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "admin/projects/create";
        }

        try {
            ProjectDTO newProject = projectService.createProject(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + newProject.getName() + "' created successfully!");
            return "redirect:/admin/projects/" + newProject.getId();
        } catch (IllegalArgumentException e) {
            result.rejectValue("teamId", "error.projectRequest", e.getMessage());
            return "admin/projects/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("projectRequest")) {
                UpdateProjectRequest req = projectService.getUpdateProjectRequest(id);
                model.addAttribute("projectRequest", req);
            }
            model.addAttribute("projectId", id);
            return "admin/projects/edit";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/projects/" + id;
        }
    }

    @PostMapping("/edit/{id}")
    public String updateProject(
            @PathVariable Long id,
            @Valid @ModelAttribute("projectRequest") UpdateProjectRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("projectId", id);
            return "admin/projects/edit";
        }

        try {
            ProjectDTO updated = projectService.updateProject(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + updated.getName() + "' updated successfully!");
            return "redirect:/admin/projects/" + updated.getId();
        } catch (IllegalArgumentException | IllegalStateException e) { // Bắt thêm IllegalStateException
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/projects/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectService.cancelProject(id);
            redirectAttributes.addFlashAttribute("successMessage", "Project has been cancelled successfully.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while cancelling the project.");
        }
        return "redirect:/admin/projects";
    }

    @GetMapping("/api/teams/{teamId}/users")
    @ResponseBody
    public ResponseEntity<List<UserSelectionDTO>> getUsersByTeamApi(@PathVariable Long teamId) {
        List<UserSelectionDTO> users = teamService.getActiveUsersByTeam(teamId);
        return ResponseEntity.ok(users);
    }
}