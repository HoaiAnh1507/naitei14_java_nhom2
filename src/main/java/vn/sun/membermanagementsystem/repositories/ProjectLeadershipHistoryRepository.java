package vn.sun.membermanagementsystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.ProjectLeadershipHistory;
import vn.sun.membermanagementsystem.entities.User;

import java.util.Optional;

@Repository
public interface ProjectLeadershipHistoryRepository extends JpaRepository<ProjectLeadershipHistory, Long> {

    Optional<ProjectLeadershipHistory> findByProjectAndEndedAtIsNull(Project project);

    boolean existsByProjectAndLeaderAndEndedAtIsNull(Project project, User leader);
}
