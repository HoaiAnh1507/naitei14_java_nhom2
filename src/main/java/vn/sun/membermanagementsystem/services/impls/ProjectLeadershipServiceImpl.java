package vn.sun.membermanagementsystem.services.impls;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.entities.*;
import vn.sun.membermanagementsystem.enums.MembershipStatus;
import vn.sun.membermanagementsystem.repositories.ProjectLeadershipHistoryRepository;
import vn.sun.membermanagementsystem.repositories.TeamMemberRepository;
import vn.sun.membermanagementsystem.repositories.UserRepository;
import vn.sun.membermanagementsystem.services.ProjectLeadershipService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectLeadershipServiceImpl implements ProjectLeadershipService {

    private final ProjectLeadershipHistoryRepository leadershipRepo;
    private final UserRepository userRepo;
    private final TeamMemberRepository teamMemberRepo;

    @Transactional
    public void endAllLeadership(Project project) {
        project.getLeadershipHistory().stream()
                .filter(h -> h.getEndedAt() == null)
                .forEach(h -> {
                    h.setEndedAt(LocalDateTime.now());
                    leadershipRepo.save(h);
                });
    }

    @Transactional
    public void updateLeader(Project project, Long requestedLeaderId, Team team) {
        Optional<ProjectLeadershipHistory> currentOpt = leadershipRepo.findByProjectAndEndedAtIsNull(project);

        if (requestedLeaderId == null) {
            currentOpt.ifPresent(curr -> {
                curr.setEndedAt(LocalDateTime.now());
                leadershipRepo.save(curr);
            });
            return;
        }

        if (currentOpt.isPresent() && Objects.equals(currentOpt.get().getLeader().getId(), requestedLeaderId)) {
            return;
        }

        User newLeader = userRepo.findById(requestedLeaderId)
                .orElseThrow(() -> new EntityNotFoundException("Leader not found"));

        if (team != null && !teamMemberRepo.existsByUserAndTeamAndStatus(newLeader, team, MembershipStatus.ACTIVE)) {
            throw new IllegalArgumentException("Selected leader is not an active member of the chosen team");
        }

        currentOpt.ifPresent(curr -> {
            curr.setEndedAt(LocalDateTime.now());
            leadershipRepo.save(curr);
        });

        if (!leadershipRepo.existsByProjectAndLeaderAndEndedAtIsNull(project, newLeader)) {
            ProjectLeadershipHistory newHist = new ProjectLeadershipHistory();
            newHist.setProject(project);
            newHist.setLeader(newLeader);
            newHist.setStartedAt(LocalDateTime.now());
            leadershipRepo.save(newHist);
        }
    }
}