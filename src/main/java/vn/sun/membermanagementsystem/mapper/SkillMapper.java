package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.sun.membermanagementsystem.dto.request.CreateSkillRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateSkillRequest;
import vn.sun.membermanagementsystem.dto.response.SkillDTO;
import vn.sun.membermanagementsystem.entities.Skill;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    SkillDTO toDTO(Skill skill);
    
    List<SkillDTO> toDTOList(List<Skill> skills);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userSkills", ignore = true)
    Skill toEntity(CreateSkillRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userSkills", ignore = true)
    void updateEntity(UpdateSkillRequest request, @MappingTarget Skill skill);
}
