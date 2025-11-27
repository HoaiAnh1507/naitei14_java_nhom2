package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.sun.membermanagementsystem.dto.request.CreateSkillRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateSkillRequest;
import vn.sun.membermanagementsystem.dto.response.SkillDTO;

public interface SkillService {
    
    Page<SkillDTO> getAllSkills(Pageable pageable);
    
    SkillDTO getSkillById(Long id);
    
    SkillDTO createSkill(CreateSkillRequest request);
    
    SkillDTO updateSkill(Long id, UpdateSkillRequest request);
    
    void deleteSkill(Long id);
}
