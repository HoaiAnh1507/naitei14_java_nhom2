package vn.sun.membermanagementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.sun.membermanagementsystem.enums.UserRole;
import vn.sun.membermanagementsystem.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for displaying user in list/table view.
 * Contains only essential fields for list display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListItemDTO {
    private Long id;
    private String name;
    private String email;
    private LocalDate birthday;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private String activeTeam;
    private PositionInfo currentPosition;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionInfo {
        private Long id;
        private String name;
        private String abbreviation;
    }
}
