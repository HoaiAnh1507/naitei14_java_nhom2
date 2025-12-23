package vn.sun.membermanagementsystem.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.sun.membermanagementsystem.services.ProjectService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectStatusScheduler {

    private final ProjectService projectService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleTask() {
        log.info("Running daily scheduled task...");
        runUpdate();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("App started. Running initial project status check...");
        runUpdate();
    }

    private void runUpdate() {
        try {
            projectService.updateAllProjectStatuses();
            log.info("Project status update task finished.");
        } catch (Exception e) {
            log.error("Error occurred during project status update task", e);
        }
    }
}