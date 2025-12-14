package vn.sun.membermanagementsystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.entities.TeamMember;
import vn.sun.membermanagementsystem.entities.User;
import vn.sun.membermanagementsystem.enums.MembershipStatus;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

        @Query("SELECT tm FROM TeamMember tm " +
                        "JOIN FETCH tm.team t " +
                        "WHERE tm.user.id = :userId " +
                        "AND tm.status = 'ACTIVE' " +
                        "AND tm.leftAt IS NULL " +
                        "AND t.deletedAt IS NULL")
        TeamMember findActiveTeamByUserId(@Param("userId") Long userId);

        @Query("SELECT tm FROM TeamMember tm " +
                        "JOIN FETCH tm.team t " +
                        "WHERE tm.user.id IN :userIds " +
                        "AND tm.status = 'ACTIVE' " +
                        "AND tm.leftAt IS NULL " +
                        "AND t.deletedAt IS NULL")
        List<TeamMember> findActiveTeamsByUserIds(@Param("userIds") List<Long> userIds);

        boolean existsByUserAndTeamAndStatus(User user, Team team, MembershipStatus status);

        @Query("SELECT tm.user FROM TeamMember tm " +
                        "JOIN tm.team t " +
                        "WHERE t.id = :teamId " +
                        "AND tm.status = vn.sun.membermanagementsystem.enums.MembershipStatus.ACTIVE " +
                        "AND tm.leftAt IS NULL " +
                        "AND t.deletedAt IS NULL")
        List<User> findActiveUsersByTeamId(@Param("teamId") Long teamId);

        @Query("SELECT tm FROM TeamMember tm " +
                        "JOIN FETCH tm.user " +
                        "WHERE tm.team.id = :teamId " +
                        "AND tm.leftAt IS NULL")
        List<TeamMember> findByTeamIdAndLeftAtIsNull(@Param("teamId") Long teamId);

        @Query("SELECT tm FROM TeamMember tm " +
                        "JOIN FETCH tm.user u " +
                        "WHERE tm.team.id = :teamId " +
                        "AND tm.status = vn.sun.membermanagementsystem.enums.MembershipStatus.ACTIVE " +
                        "AND tm.leftAt IS NULL")
        Page<TeamMember> findActiveTeamMembersByTeamId(@Param("teamId") Long teamId, Pageable pageable);
}
