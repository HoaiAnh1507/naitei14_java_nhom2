package vn.sun.membermanagementsystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.sun.membermanagementsystem.dto.response.TeamDTO;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.services.TeamService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class ClientTeamController {

    private final TeamService teamService;

    /**
     * GET /api/teams - List all teams with pagination
     *
     * @param page    Page number (default: 0)
     * @param size    Page size (default: 10)
     * @param sortBy  Sort field (default: "name")
     * @param sortDir Sort direction (default: "asc")
     * @param keyword Search keyword for team name (optional)
     * @return Page of teams with pagination metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword) {

        log.info("Client API: Getting all teams - page: {}, size: {}, sortBy: {}, sortDir: {}, keyword: {}",
                page, size, sortBy, sortDir, keyword);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TeamDTO> teamPage = teamService.getAllTeamsWithPagination(pageable, keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("teams", teamPage.getContent());
        response.put("currentPage", teamPage.getNumber());
        response.put("totalPages", teamPage.getTotalPages());
        response.put("totalElements", teamPage.getTotalElements());
        response.put("pageSize", teamPage.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/teams/{id} - Get team detail
     *
     * @param id Team ID
     * @return Team detail information
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamDetailDTO> getTeamDetail(@PathVariable Long id) {
        log.info("Client API: Getting team detail for ID: {}", id);

        try {
            TeamDetailDTO teamDetail = teamService.getTeamDetail(id);
            return ResponseEntity.ok(teamDetail);
        } catch (ResourceNotFoundException e) {
            log.error("Team not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/teams/{id}/members - List members in a team with pagination
     *
     * @param id   Team ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Page of team members with pagination metadata
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> getTeamMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Client API: Getting members for team ID: {}, page: {}, size: {}", id, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
            Page<TeamDetailDTO.TeamMemberDTO> memberPage = teamService.getTeamMembersWithPagination(id, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("members", memberPage.getContent());
            response.put("currentPage", memberPage.getNumber());
            response.put("totalPages", memberPage.getTotalPages());
            response.put("totalElements", memberPage.getTotalElements());
            response.put("pageSize", memberPage.getSize());

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.error("Team not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
