package vn.sun.membermanagementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    private String name;
    private String abbreviation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long teamId;
    private String teamName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
