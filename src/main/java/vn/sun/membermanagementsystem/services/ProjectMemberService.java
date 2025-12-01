package vn.sun.membermanagementsystem.services;

import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.Team;
import vn.sun.membermanagementsystem.entities.User;

import java.util.List;

public interface ProjectMemberService {
    void removeAllMembers(Project project);
    void syncMembers(Project project, List<Long> requestedMemberIds, Long leaderId, Team team);
    void ensureUserIsActiveMember(Project project, Long userId, Team team);
    void ensureUserIsActiveMember(Project project, User user, Team team);
}