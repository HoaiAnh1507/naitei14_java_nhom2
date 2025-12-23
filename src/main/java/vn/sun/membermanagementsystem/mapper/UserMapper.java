package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.sun.membermanagementsystem.dto.request.UserUpdateDTO;
import vn.sun.membermanagementsystem.dto.response.*;
import vn.sun.membermanagementsystem.entities.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // ===== UserListItemDTO mappings =====
    @Mapping(target = "activeTeam", expression = "java(getActiveTeamName(user))")
    @Mapping(target = "currentPosition", expression = "java(mapCurrentPositionForList(user))")
    UserListItemDTO toListItemDTO(User user);

    List<UserListItemDTO> toListItemDTOList(List<User> users);

    // ===== UserProfileDetailDTO mappings =====
    @Mapping(target = "activeTeam", expression = "java(getActiveTeamName(user))")
    @Mapping(target = "activeProjects", expression = "java(mapActiveProjectsForDetail(user))")
    @Mapping(target = "currentPosition", expression = "java(mapCurrentPositionForDetail(user))")
    @Mapping(target = "skills", expression = "java(mapSkillsForDetail(user))")
    UserProfileDetailDTO toProfileDetailDTO(User user);

    // ===== UserUpdateDTO mappings (for edit form) =====
    @Mapping(target = "positionId", expression = "java(getCurrentPositionId(user))")
    @Mapping(target = "positionName", expression = "java(getCurrentPositionName(user))")
    @Mapping(target = "skills", expression = "java(mapSkillsForUpdateForm(user))")
    @Mapping(target = "password", ignore = true)
    UserUpdateDTO toUpdateDTO(User user);

    // ===== Helper methods =====
    default String getActiveTeamName(User user) {
        if (user.getTeamMemberships() == null || user.getTeamMemberships().isEmpty()) {
            return null;
        }
        return user.getTeamMemberships().stream()
                .filter(tm -> tm.getStatus() != null && "ACTIVE".equals(tm.getStatus().name()) 
                        && tm.getLeftAt() == null 
                        && tm.getTeam() != null 
                        && tm.getTeam().getDeletedAt() == null)
                .map(tm -> tm.getTeam().getName())
                .findFirst()
                .orElse(null);
    }

    // For UserListItemDTO
    default UserListItemDTO.PositionInfo mapCurrentPositionForList(User user) {
        if (user.getPositionHistories() == null || user.getPositionHistories().isEmpty()) {
            return null;
        }
        return user.getPositionHistories().stream()
                .filter(ph -> ph.getEndedAt() == null && ph.getPosition() != null)
                .findFirst()
                .map(ph -> UserListItemDTO.PositionInfo.builder()
                        .id(ph.getPosition().getId())
                        .name(ph.getPosition().getName())
                        .abbreviation(ph.getPosition().getAbbreviation())
                        .build())
                .orElse(null);
    }

    // For UserProfileDetailDTO
    default List<UserProfileDetailDTO.ProjectInfo> mapActiveProjectsForDetail(User user) {
        if (user.getProjectMemberships() == null || user.getProjectMemberships().isEmpty()) {
            return List.of();
        }
        return user.getProjectMemberships().stream()
                .filter(pm -> pm.getStatus() != null && "ACTIVE".equals(pm.getStatus().name())
                        && pm.getLeftAt() == null
                        && pm.getProject() != null
                        && pm.getProject().getDeletedAt() == null)
                .map(pm -> UserProfileDetailDTO.ProjectInfo.builder()
                        .id(pm.getProject().getId())
                        .name(pm.getProject().getName())
                        .abbreviation(pm.getProject().getAbbreviation())
                        .status(pm.getProject().getStatus() != null ? pm.getProject().getStatus().name() : null)
                        .build())
                .toList();
    }

    default UserProfileDetailDTO.PositionInfo mapCurrentPositionForDetail(User user) {
        if (user.getPositionHistories() == null || user.getPositionHistories().isEmpty()) {
            return null;
        }
        return user.getPositionHistories().stream()
                .filter(ph -> ph.getEndedAt() == null && ph.getPosition() != null)
                .findFirst()
                .map(ph -> UserProfileDetailDTO.PositionInfo.builder()
                        .id(ph.getPosition().getId())
                        .name(ph.getPosition().getName())
                        .abbreviation(ph.getPosition().getAbbreviation())
                        .build())
                .orElse(null);
    }

    default List<UserProfileDetailDTO.SkillInfo> mapSkillsForDetail(User user) {
        if (user.getUserSkills() == null || user.getUserSkills().isEmpty()) {
            return List.of();
        }
        return user.getUserSkills().stream()
                .filter(us -> us.getSkill() != null && us.getSkill().getDeletedAt() == null)
                .map(us -> UserProfileDetailDTO.SkillInfo.builder()
                        .skillId(us.getSkill().getId())
                        .skillName(us.getSkill().getName())
                        .level(us.getLevel())
                        .usedYearNumber(us.getUsedYearNumber())
                        .build())
                .toList();
    }

    // For UserUpdateDTO (edit form)
    default Long getCurrentPositionId(User user) {
        if (user.getPositionHistories() == null || user.getPositionHistories().isEmpty()) {
            return null;
        }
        return user.getPositionHistories().stream()
                .filter(ph -> ph.getEndedAt() == null && ph.getPosition() != null)
                .findFirst()
                .map(ph -> ph.getPosition().getId())
                .orElse(null);
    }

    default String getCurrentPositionName(User user) {
        if (user.getPositionHistories() == null || user.getPositionHistories().isEmpty()) {
            return null;
        }
        return user.getPositionHistories().stream()
                .filter(ph -> ph.getEndedAt() == null && ph.getPosition() != null)
                .findFirst()
                .map(ph -> ph.getPosition().getName())
                .orElse(null);
    }

    default List<UserUpdateDTO.SkillEntry> mapSkillsForUpdateForm(User user) {
        if (user.getUserSkills() == null || user.getUserSkills().isEmpty()) {
            return List.of();
        }
        return user.getUserSkills().stream()
                .filter(us -> us.getSkill() != null && us.getSkill().getDeletedAt() == null)
                .map(us -> UserUpdateDTO.SkillEntry.builder()
                        .skillId(us.getSkill().getId())
                        .skillName(us.getSkill().getName())
                        .level(us.getLevel())
                        .usedYearNumber(us.getUsedYearNumber())
                        .build())
                .toList();
    }
}
