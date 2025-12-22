package vn.sun.membermanagementsystem.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sun.membermanagementsystem.dto.request.CreateTeamRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateTeamRequest;
import vn.sun.membermanagementsystem.dto.request.csv.CsvImportResult;
import vn.sun.membermanagementsystem.dto.request.csv.CsvPreviewResult;
import vn.sun.membermanagementsystem.dto.response.TeamDTO;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.dto.response.TeamStatisticsDTO;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.exception.BadRequestException;
import vn.sun.membermanagementsystem.exception.DuplicateResourceException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.services.TeamMemberService;
import vn.sun.membermanagementsystem.services.TeamService;
import vn.sun.membermanagementsystem.services.UserService;
import vn.sun.membermanagementsystem.services.csv.impls.TeamCsvExportService;
import vn.sun.membermanagementsystem.services.csv.impls.TeamCsvImportService;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final TeamService teamService;
    private final UserService userService;
    private final TeamMemberService teamMemberService;
    private final TeamCsvExportService teamCsvExportService;
    private final TeamCsvImportService teamCsvImportService;

    @GetMapping
    public String teamList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TeamDTO> teamPage = teamService.getAllTeamsWithPagination(pageable, keyword);

        model.addAttribute("teams", teamPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", teamPage.getTotalPages());
        model.addAttribute("totalItems", teamPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);

        return "admin/teams/index";
    }

    @GetMapping("/create")
    public String showCreateTeamForm(Model model) {
        // Get all active users for leader selection
        model.addAttribute("users", userService.getAllUsers());
        return "admin/teams/create";
    }

    @PostMapping
    public String createTeam(@Valid @ModelAttribute CreateTeamRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Please check the form for errors");
            model.addAttribute("errors", result);
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("team", request);
            return "admin/teams/create";
        }

        try {
            TeamDTO createdTeam = teamService.createTeam(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Team '" + createdTeam.getName() + "' has been created successfully!");
            return "redirect:/admin/teams/" + createdTeam.getId();
        } catch (DuplicateResourceException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("team", request);
            return "admin/teams/create";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("team", request);
            return "admin/teams/create";
        }
    }

    @GetMapping("/{id}")
    public String viewTeam(@PathVariable Long id, Model model) {
        try {
            TeamDetailDTO team = teamService.getTeamDetail(id);
            TeamStatisticsDTO statistics = teamService.getTeamStatistics(id);

            model.addAttribute("team", team);
            model.addAttribute("statistics", statistics);

            return "admin/teams/view";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/teams";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditTeamForm(@PathVariable Long id, Model model) {
        try {
            TeamDetailDTO team = teamService.getTeamDetail(id);
            TeamStatisticsDTO statistics = teamService.getTeamStatistics(id);

            model.addAttribute("team", team);
            model.addAttribute("statistics", statistics);
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("availableUsers", userService.getAllUsers()); // For member management
            model.addAttribute("allTeams", teamService.getAllTeams()); // For transfer modal

            return "admin/teams/edit";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/teams";
        }
    }

    @PostMapping("/{id}")
    public String updateTeam(@PathVariable Long id,
            @Valid @ModelAttribute UpdateTeamRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Please check the form for errors");
            model.addAttribute("errors", result);
            model.addAttribute("users", userService.getAllUsers());

            try {
                TeamDetailDTO team = teamService.getTeamDetail(id);
                model.addAttribute("team", team);
            } catch (ResourceNotFoundException e) {
                return "redirect:/admin/teams";
            }

            return "admin/teams/edit";
        }

        try {
            TeamDTO updatedTeam = teamService.updateTeam(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Team '" + updatedTeam.getName() + "' has been updated successfully!");
            return "redirect:/admin/teams/" + id;
        } catch (DuplicateResourceException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("users", userService.getAllUsers());

            try {
                TeamDetailDTO team = teamService.getTeamDetail(id);
                model.addAttribute("team", team);
            } catch (ResourceNotFoundException ex) {
                return "redirect:/admin/teams";
            }

            return "admin/teams/edit";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/teams";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            model.addAttribute("users", userService.getAllUsers());

            try {
                TeamDetailDTO team = teamService.getTeamDetail(id);
                model.addAttribute("team", team);
            } catch (ResourceNotFoundException ex) {
                return "redirect:/admin/teams";
            }

            return "admin/teams/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteTeam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            teamService.deleteTeam(id);
            redirectAttributes.addFlashAttribute("successMessage", "Team has been deleted successfully!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team not found!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
        }

        return "redirect:/admin/teams";
    }

    @PostMapping("/{teamId}/members/add")
    @ResponseBody
    public String addMemberToTeam(@PathVariable Long teamId,
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {
        try {
            teamService.addMemberToTeam(teamId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Member added successfully!");
            return "success";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "error: " + e.getMessage();
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team or user not found!");
            return "error: " + e.getMessage();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{teamId}/members/add-bulk")
    @ResponseBody
    public Map<String, Object> addMembersToTeam(@PathVariable Long teamId,
            @RequestBody Map<String, List<Long>> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Long> userIds = request.get("userIds");
            if (userIds == null || userIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "No users selected");
                return response;
            }

            int addedCount = teamService.addMembersToTeam(teamId, userIds);
            response.put("success", true);
            response.put("addedCount", addedCount);
            return response;
        } catch (BadRequestException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", "Team not found!");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/{teamId}/members/{userId}/remove")
    @ResponseBody
    public String removeMemberFromTeam(@PathVariable Long teamId,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {
        try {
            teamService.removeMemberFromTeam(teamId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Member removed successfully!");
            return "success";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "error: " + e.getMessage();
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Team or user not found!");
            return "error: " + e.getMessage();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{teamId}/members/{userId}/transfer")
    @ResponseBody
    public Map<String, Object> transferMember(@PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long newTeamId = request.get("newTeamId");
            if (newTeamId == null) {
                response.put("success", false);
                response.put("message", "Destination team not specified");
                return response;
            }

            if (newTeamId.equals(teamId)) {
                response.put("success", false);
                response.put("message", "Cannot transfer to the same team");
                return response;
            }

            teamMemberService.transferMember(userId, newTeamId);
            response.put("success", true);
            response.put("message", "Member transferred successfully!");
            return response;
        } catch (BadRequestException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", "Team or user not found!");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/export")
    public void exportTeams(HttpServletResponse response) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "teams_export_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        teamCsvExportService.exportToCsv(response.getOutputStream());
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        log.info("Downloading team import template");

        String csvContent = teamCsvImportService.generateSampleCsv();
        byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Add BOM for Excel UTF-8 support
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"teams_import_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    @GetMapping("/import")
    public String showImportPage(Model model) {
        model.addAttribute("entityType", "Team");
        return "admin/teams/import";
    }

    @PostMapping(value = "/import/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CsvPreviewResult> previewImport(@RequestParam("file") MultipartFile file) {
        log.info("Previewing CSV import for teams, file: {}", file.getOriginalFilename());
        CsvPreviewResult result = teamCsvImportService.previewCsv(file);
        log.info("Preview result - totalRows: {}, validRows: {}, invalidRows: {}, hasErrors: {}, fileError: {}",
                result.getTotalRows(), result.getValidRows(), result.getInvalidRows(),
                result.isHasErrors(), result.getFileError());
        if (result.getHeaders() != null) {
            log.info("Headers: {}", String.join(", ", result.getHeaders()));
        }
        if (result.getRows() != null) {
            log.info("Rows count: {}", result.getRows().size());
            result.getRows().forEach(row -> {
                log.info("Row {}: valid={}, data={}, errors={}",
                        row.getRowNumber(), row.isValid(),
                        row.getData() != null ? String.join("|", row.getData()) : "null",
                        row.getErrors());
            });
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/import")
    public String importTeams(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        log.info("Importing teams from CSV file: {}", file.getOriginalFilename());

        CsvImportResult<Team> result = teamCsvImportService.importFromCsv(file);

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("importErrors", result.getErrors());
            redirectAttributes.addFlashAttribute("errorCount", result.getErrorCount());

            if (result.isRolledBack()) {
                redirectAttributes.addFlashAttribute("rolledBack", true);
                redirectAttributes.addFlashAttribute("errorMessage",
                        String.format(
                                "Import failed. %d error(s) found. No teams were imported. Please fix the errors and try again.",
                                result.getErrorCount()));
            }
        }

        if (!result.isRolledBack()) {
            redirectAttributes.addFlashAttribute("successCount", result.getSuccessCount());
            redirectAttributes.addFlashAttribute("totalRows", result.getTotalRows());

            if (result.getSuccessCount() > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Successfully imported %d team(s)",
                                result.getSuccessCount()));
            }
        }

        return "redirect:/admin/teams/import";
    }
}
