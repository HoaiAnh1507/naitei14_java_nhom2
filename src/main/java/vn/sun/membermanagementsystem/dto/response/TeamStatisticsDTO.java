package vn.sun.membermanagementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatisticsDTO {

    private Long teamId;
    private String teamName;
    private long totalMembers;
    private long activeMembers;
    private long inactiveMembers;
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private TeamLeaderDTO currentLeader;
}
