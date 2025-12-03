package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.sun.membermanagementsystem.dto.response.TeamDTO;
import vn.sun.membermanagementsystem.dto.request.CreateTeamRequest;
import vn.sun.membermanagementsystem.dto.request.UpdateTeamRequest;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.dto.response.TeamStatisticsDTO;
import vn.sun.membermanagementsystem.dto.response.UserSelectionDTO;
import vn.sun.membermanagementsystem.entities.Team;

import java.util.List;
import java.util.Optional;

public interface TeamService {

    Optional<TeamDTO> getTeamById(Long id);

    Team getRequiredTeam(Long id);

    TeamDTO createTeam(CreateTeamRequest request);

    TeamDTO updateTeam(Long id, UpdateTeamRequest request);

    boolean deleteTeam(Long id);

    TeamDetailDTO getTeamDetail(Long id);

    List<TeamDTO> getAllTeams();

    Page<TeamDTO> getAllTeamsWithPagination(Pageable pageable);

    TeamStatisticsDTO getTeamStatistics(Long teamId);

    List<UserSelectionDTO> getActiveUsersByTeam(Long teamId);

    void addMemberToTeam(Long teamId, Long userId);

    int addMembersToTeam(Long teamId, List<Long> userIds);

    void removeMemberFromTeam(Long teamId, Long userId);
}
