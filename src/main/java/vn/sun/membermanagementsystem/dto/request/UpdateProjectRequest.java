// UpdateProjectRequest.java
package vn.sun.membermanagementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProjectRequest {
    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 50)
    private String abbreviation;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Long teamId;

    private Long leaderId;
    private List<Long> memberIds;
}
