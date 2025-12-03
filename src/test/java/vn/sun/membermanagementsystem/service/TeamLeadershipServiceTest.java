package vn.sun.membermanagementsystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.sun.membermanagementsystem.dto.response.TeamDetailDTO;
import vn.sun.membermanagementsystem.dto.response.TeamLeaderDTO;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.entities.TeamLeadershipHistory;
import vn.sun.membermanagementsystem.entities.TeamMember;
import vn.sun.membermanagementsystem.entities.User;
import vn.sun.membermanagementsystem.enums.MembershipStatus;
import vn.sun.membermanagementsystem.exception.BadRequestException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.repositories.TeamLeadershipHistoryRepository;
import vn.sun.membermanagementsystem.repositories.TeamMemberRepository;
import vn.sun.membermanagementsystem.repositories.TeamRepository;
import vn.sun.membermanagementsystem.repositories.UserRepository;
import vn.sun.membermanagementsystem.services.impls.TeamLeadershipServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamLeadershipService Unit Tests")
public class TeamLeadershipServiceTest {

    @Mock
    private TeamLeadershipHistoryRepository leadershipRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TeamLeadershipServiceImpl leadershipService;

    private Team testTeam;
    private User testUser;
    private TeamMember testMembership;
    private TeamLeadershipHistory testLeadership;

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setId(1L);
        testTeam.setName("Test Team");
        testTeam.setCreatedAt(LocalDateTime.now());

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testMembership = new TeamMember();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setTeam(testTeam);
        testMembership.setStatus(MembershipStatus.ACTIVE);
        testMembership.setJoinedAt(LocalDateTime.now());

        testLeadership = new TeamLeadershipHistory();
        testLeadership.setId(1L);
        testLeadership.setTeam(testTeam);
        testLeadership.setLeader(testUser);
        testLeadership.setStartedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Assign leader successfully when user is not a member yet")
    void assignLeader_UserNotMember_Success() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testUser));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findActiveTeamByUserId(1L)).thenReturn(null);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(testMembership);
        when(leadershipRepository.save(any(TeamLeadershipHistory.class))).thenReturn(testLeadership);

        TeamLeaderDTO result = leadershipService.assignLeader(1L, 1L);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(teamMemberRepository, times(1)).save(any(TeamMember.class));
        verify(leadershipRepository, times(1)).save(any(TeamLeadershipHistory.class));
    }

    @Test
    @DisplayName("Assign leader successfully when user is already a member")
    void assignLeader_UserAlreadyMember_Success() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testUser));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findActiveTeamByUserId(1L)).thenReturn(testMembership);
        when(leadershipRepository.save(any(TeamLeadershipHistory.class))).thenReturn(testLeadership);

        TeamLeaderDTO result = leadershipService.assignLeader(1L, 1L);

        assertNotNull(result);
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
        verify(leadershipRepository, times(1)).save(any(TeamLeadershipHistory.class));
    }

    @Test
    @DisplayName("Assign leader fails when team not found")
    void assignLeader_TeamNotFound_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadershipService.assignLeader(1L, 1L));

        verify(leadershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign leader fails when user not found")
    void assignLeader_UserNotFound_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadershipService.assignLeader(1L, 1L));

        verify(leadershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign leader fails when team already has active leader")
    void assignLeader_TeamHasLeader_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testUser));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));

        assertThrows(BadRequestException.class, () -> leadershipService.assignLeader(1L, 1L));

        verify(leadershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assign leader fails when user is member of another team")
    void assignLeader_UserInAnotherTeam_ThrowsException() {
        Team anotherTeam = new Team();
        anotherTeam.setId(2L);

        TeamMember anotherMembership = new TeamMember();
        anotherMembership.setTeam(anotherTeam);
        anotherMembership.setUser(testUser);

        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testUser));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findActiveTeamByUserId(1L)).thenReturn(anotherMembership);

        assertThrows(BadRequestException.class, () -> leadershipService.assignLeader(1L, 1L));

        verify(leadershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change leader successfully")
    void changeLeader_Success() {
        User newLeader = new User();
        newLeader.setId(2L);
        newLeader.setName("New Leader");
        newLeader.setEmail("newleader@example.com");

        TeamMember newMembership = new TeamMember();
        newMembership.setTeam(testTeam);
        newMembership.setUser(newLeader);
        newMembership.setStatus(MembershipStatus.ACTIVE);

        TeamLeadershipHistory newLeadership = new TeamLeadershipHistory();
        newLeadership.setTeam(testTeam);
        newLeadership.setLeader(newLeader);
        newLeadership.setStartedAt(LocalDateTime.now());

        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(newLeader));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));
        when(teamMemberRepository.findActiveTeamByUserId(2L)).thenReturn(newMembership);
        when(leadershipRepository.save(any(TeamLeadershipHistory.class))).thenReturn(newLeadership);

        TeamLeaderDTO result = leadershipService.changeLeader(1L, 2L);

        assertNotNull(result);
        assertEquals(newLeader.getId(), result.getUserId());
        assertNotNull(testLeadership.getEndedAt());
        verify(leadershipRepository, times(2)).save(any(TeamLeadershipHistory.class));
    }

    @Test
    @DisplayName("Change leader fails when new leader is not an active member")
    void changeLeader_UserNotActiveMember_ThrowsException() {
        User newLeader = new User();
        newLeader.setId(2L);

        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(newLeader));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));
        when(teamMemberRepository.findActiveTeamByUserId(2L)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> leadershipService.changeLeader(1L, 2L));
    }

    @Test
    @DisplayName("Change leader fails when user is already the current leader")
    void changeLeader_SameLeader_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testUser));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));

        assertThrows(BadRequestException.class, () -> leadershipService.changeLeader(1L, 1L));
    }

    @Test
    @DisplayName("Remove leader successfully")
    void removeLeader_Success() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));

        leadershipService.removeLeader(1L);

        assertNotNull(testLeadership.getEndedAt());
        verify(leadershipRepository, times(1)).save(testLeadership);
    }

    @Test
    @DisplayName("Remove leader fails when team has no active leader")
    void removeLeader_NoActiveLeader_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> leadershipService.removeLeader(1L));

        verify(leadershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get current leader successfully")
    void getCurrentLeader_Success() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.of(testLeadership));

        TeamLeaderDTO result = leadershipService.getCurrentLeader(1L);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testUser.getName(), result.getName());
    }

    @Test
    @DisplayName("Get current leader returns null when no active leader")
    void getCurrentLeader_NoActiveLeader_ReturnsNull() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(leadershipRepository.findActiveLeaderByTeamId(1L)).thenReturn(Optional.empty());

        TeamLeaderDTO result = leadershipService.getCurrentLeader(1L);

        assertNull(result);
    }

    @Test
    @DisplayName("Get leadership history successfully")
    void getLeadershipHistory_Success() {
        TeamLeadershipHistory oldLeadership = new TeamLeadershipHistory();
        oldLeadership.setTeam(testTeam);
        oldLeadership.setLeader(testUser);
        oldLeadership.setStartedAt(LocalDateTime.now().minusDays(30));
        oldLeadership.setEndedAt(LocalDateTime.now().minusDays(10));

        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testTeam));
        when(leadershipRepository.findByTeamIdOrderByStartedAtDesc(1L))
                .thenReturn(Arrays.asList(testLeadership, oldLeadership));

        List<TeamDetailDTO.TeamLeadershipHistoryDTO> result = leadershipService.getLeadershipHistory(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsCurrent());
        assertFalse(result.get(1).getIsCurrent());
    }

    @Test
    @DisplayName("Get leadership history for team not found throws exception")
    void getLeadershipHistory_TeamNotFound_ThrowsException() {
        when(teamRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadershipService.getLeadershipHistory(1L));
    }
}
