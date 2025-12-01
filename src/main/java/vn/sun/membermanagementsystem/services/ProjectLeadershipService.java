package vn.sun.membermanagementsystem.services;

import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.Team;

public interface ProjectLeadershipService {

    void endAllLeadership(Project project);
    void updateLeader(Project project, Long requestedLeaderId, Team team);
}