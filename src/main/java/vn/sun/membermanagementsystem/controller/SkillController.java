package vn.sun.membermanagementsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.sun.membermanagementsystem.dto.request.CreateSkillRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateSkillRequest;
import vn.sun.membermanagementsystem.dto.response.SkillDTO;
import vn.sun.membermanagementsystem.services.SkillService;

import java.util.List;

@Controller
@RequestMapping("/admin/skills")
@RequiredArgsConstructor
public class SkillController {
    
    private final SkillService skillService;
    
    @GetMapping
    public String listSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SkillDTO> skills = skillService.getAllSkills(pageable);
        
        model.addAttribute("skills", skills);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", skills.getTotalPages());
        model.addAttribute("totalItems", skills.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("currentPageSize", size);
        model.addAttribute("pageSizeOptions", List.of(10, 25, 50, 100));
        
        return "admin/skills/index";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("skill", new CreateSkillRequest());
        return "admin/skills/create";
    }
    
    @PostMapping
    public String createSkill(
            @Valid @ModelAttribute("skill") CreateSkillRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            return "admin/skills/create";
        }
        
        try {
            skillService.createSkill(request);
            redirectAttributes.addFlashAttribute("successMessage", "Skill created successfully!");
            return "redirect:/admin/skills";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/skills/create";
        }
    }
    
    @GetMapping("/{id}")
    public String viewSkill(@PathVariable Long id, Model model) {
        try {
            SkillDTO skill = skillService.getSkillById(id);
            model.addAttribute("skill", skill);
            return "admin/skills/view";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/skills";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            SkillDTO skillDTO = skillService.getSkillById(id);
            
            UpdateSkillRequest request = new UpdateSkillRequest();
            request.setName(skillDTO.getName());
            request.setDescription(skillDTO.getDescription());
            
            model.addAttribute("skill", request);
            model.addAttribute("skillId", id);
            return "admin/skills/edit";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/skills";
        }
    }
    
    @PostMapping("/{id}")
    public String updateSkill(
            @PathVariable Long id,
            @Valid @ModelAttribute("skill") UpdateSkillRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("skillId", id);
            return "admin/skills/edit";
        }
        
        try {
            skillService.updateSkill(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Skill updated successfully!");
            return "redirect:/admin/skills";
        } catch (Exception e) {
            model.addAttribute("skillId", id);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/skills/edit";
        }
    }
    
    @DeleteMapping("/{id}")
    @ResponseBody
    public String deleteSkill(@PathVariable Long id) {
        try {
            skillService.deleteSkill(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}
