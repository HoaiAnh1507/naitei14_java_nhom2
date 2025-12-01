package vn.sun.membermanagementsystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.ProjectMember;
import vn.sun.membermanagementsystem.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectAndUserAndStatus(Project project, User user, ProjectMember.MemberStatus status);

    Optional<ProjectMember> findByProjectAndUserAndStatus(Project project, User user, ProjectMember.MemberStatus status);

    List<ProjectMember> findByProjectAndStatus(Project project, ProjectMember.MemberStatus status);

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

}