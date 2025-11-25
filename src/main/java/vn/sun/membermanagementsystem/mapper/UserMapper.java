package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.sun.membermanagementsystem.dto.response.*;
import vn.sun.membermanagementsystem.entities.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "activeTeam", expression = "java(getActiveTeamName(user))")
    UserSummaryDTO toSummaryDTO(User user);

    List<UserSummaryDTO> toSummaryDTOList(List<User> users);
    
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
}
