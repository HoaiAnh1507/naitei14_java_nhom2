package vn.sun.membermanagementsystem.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailDTO extends ProjectDTO {
    private List<ProjectMemberDTO> members = new ArrayList<>();
    private List<ProjectLeadershipHistoryDTO> leadershipHistory = new ArrayList<>();
}