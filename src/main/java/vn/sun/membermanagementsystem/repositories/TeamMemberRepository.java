package vn.sun.membermanagementsystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.sun.membermanagementsystem.entities.TeamMember;

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
}
