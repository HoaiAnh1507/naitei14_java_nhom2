package vn.sun.membermanagementsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sun.membermanagementsystem.dto.request.CreatePositionRequest;
import vn.sun.membermanagementsystem.dto.request.UpdatePositionRequest;
import vn.sun.membermanagementsystem.dto.response.PositionDTO;
import vn.sun.membermanagementsystem.services.PositionService;

import java.util.List;

@Controller
@RequestMapping("/admin/positions")
@RequiredArgsConstructor
@Slf4j
public class PositionController {
    
    private final PositionService positionService;
    
    @GetMapping
    public String listPositions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {
        
        log.info("GET /admin/positions - page: {}, size: {}, sortBy: {}, sortDir: {}", 
                page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PositionDTO> positions = positionService.getAllPositions(pageable);
        
        model.addAttribute("positions", positions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", positions.getTotalPages());
        model.addAttribute("totalItems", positions.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("currentPageSize", size);
        model.addAttribute("pageSizeOptions", List.of(10, 25, 50, 100));
        
        return "admin/positions/index";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        log.info("GET /admin/positions/create");
        model.addAttribute("positionRequest", new CreatePositionRequest());
        return "admin/positions/create";
    }
    
    @PostMapping
    public String createPosition(
            @Valid @ModelAttribute("positionRequest") CreatePositionRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        log.info("POST /admin/positions - request: {}", request);
        
        if (result.hasErrors()) {
            return "admin/positions/create";
        }
        
        try {
            PositionDTO createdPosition = positionService.createPosition(request);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Position '" + createdPosition.getName() + "' created successfully!");
            return "redirect:/admin/positions";
        } catch (Exception e) {
            log.error("Error creating position", e);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/positions/create";
        }
    }
    
    @GetMapping("/{id}")
    public String viewPosition(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("GET /admin/positions/{}", id);
        
        try {
            PositionDTO position = positionService.getPositionById(id);
            model.addAttribute("position", position);
            return "admin/positions/view";
        } catch (Exception e) {
            log.error("Error getting position", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/positions";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("GET /admin/positions/{}/edit", id);
        
        try {
            PositionDTO position = positionService.getPositionById(id);
            
            UpdatePositionRequest request = UpdatePositionRequest.builder()
                    .name(position.getName())
                    .abbreviation(position.getAbbreviation())
                    .build();
            
            model.addAttribute("positionId", id);
            model.addAttribute("positionRequest", request);
            model.addAttribute("position", position);
            return "admin/positions/edit";
        } catch (Exception e) {
            log.error("Error getting position for edit", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/positions";
        }
    }
    
    @PostMapping("/{id}")
    public String updatePosition(
            @PathVariable Long id,
            @Valid @ModelAttribute("positionRequest") UpdatePositionRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("POST /admin/positions/{} - request: {}", id, request);
        
        if (result.hasErrors()) {
            model.addAttribute("positionId", id);
            try {
                PositionDTO position = positionService.getPositionById(id);
                model.addAttribute("position", position);
            } catch (Exception e) {
                log.error("Error getting position", e);
            }
            return "admin/positions/edit";
        }
        
        try {
            PositionDTO updatedPosition = positionService.updatePosition(id, request);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Position '" + updatedPosition.getName() + "' updated successfully!");
            return "redirect:/admin/positions";
        } catch (Exception e) {
            log.error("Error updating position", e);
            model.addAttribute("positionId", id);
            model.addAttribute("errorMessage", e.getMessage());
            try {
                PositionDTO position = positionService.getPositionById(id);
                model.addAttribute("position", position);
            } catch (Exception ex) {
                log.error("Error getting position", ex);
            }
            return "admin/positions/edit";
        }
    }
    
    @DeleteMapping("/{id}")
    @ResponseBody
    public void deletePosition(@PathVariable Long id) {
        log.info("DELETE /admin/positions/{}", id);
        positionService.deletePosition(id);
    }
}
