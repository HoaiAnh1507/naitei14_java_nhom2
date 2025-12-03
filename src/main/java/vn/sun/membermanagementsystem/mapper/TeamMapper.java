package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.sun.membermanagementsystem.dto.request.CreateTeamRequest;
import vn.sun.membermanagementsystem.dto.response.TeamDTO;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.dto.response.TeamLeaderDTO;
import vn.sun.membermanagementsystem.entities.Team;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    TeamDTO toDTO(Team team);

    List<TeamDTO> toDTOList(List<Team> teams);

    @Mapping(target = "currentLeader", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "leadershipHistory", ignore = true)
    TeamDetailDTO toDetailDTO(Team team);

    Team toEntity(CreateTeamRequest request);

    @AfterMapping
    default void populateTeamDetailDTO(@MappingTarget TeamDetailDTO dto, Team team) {
        team.getLeadershipHistory().stream()
                .filter(lh -> lh.getEndedAt() == null)
                .findFirst()
                .ifPresent(lh -> {
                    TeamLeaderDTO leaderDTO = new TeamLeaderDTO();
                    leaderDTO.setUserId(lh.getLeader().getId());
                    leaderDTO.setName(lh.getLeader().getName());
                    leaderDTO.setStartedAt(lh.getStartedAt());
                    dto.setCurrentLeader(leaderDTO);
                });

        List<TeamDetailDTO.TeamMemberDTO> memberDTOs = team.getTeamMemberships().stream()
                .filter(tm -> tm.getLeftAt() == null)
                .map(tm -> TeamDetailDTO.TeamMemberDTO.builder()
                        .userId(tm.getUser().getId())
                        .name(tm.getUser().getName())
                        .email(tm.getUser().getEmail())
                        .position(null) 
                        .joinedAt(tm.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
        dto.setMembers(memberDTOs);
        dto.setMemberCount(memberDTOs.size());

        // Populate projects list
        List<TeamDetailDTO.ProjectSummaryDTO> projectDTOs = team.getProjects().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(p -> TeamDetailDTO.ProjectSummaryDTO.builder()
                        .projectId(p.getId())
                        .name(p.getName())
                        .abbreviation(p.getAbbreviation())
                        .status(p.getStatus() != null ? p.getStatus().toString() : null)
                        .build())
                .collect(Collectors.toList());
        dto.setProjects(projectDTOs);

        List<TeamDetailDTO.TeamLeadershipHistoryDTO> historyDTOs = team.getLeadershipHistory().stream()
                .map(lh -> TeamDetailDTO.TeamLeadershipHistoryDTO.builder()
                        .leaderId(lh.getLeader().getId())
                        .leaderName(lh.getLeader().getName())
                        .leaderEmail(lh.getLeader().getEmail())
                        .startedAt(lh.getStartedAt())
                        .endedAt(lh.getEndedAt())
                        .isCurrent(lh.getEndedAt() == null)
                        .build())
                .collect(Collectors.toList());
        dto.setLeadershipHistory(historyDTOs);
    }
}
