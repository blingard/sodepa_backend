package com.sodepa.erp.configuration;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Active la planification Spring pour les tâches récurrentes transverses
 * (notamment le rejeu automatique de l'Outbox ClickHouse du moteur de workflow)
 * avec un pool de threads dédié, verrou distribué ShedLock et arrêt gracieux.
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sodepa-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setErrorHandler(t -> log.error("[SCHEDULER] Erreur non interceptée dans une tâche planifiée : {}", t.getMessage(), t));
        return scheduler;
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }

    @EventListener
    public void onStarted(ApplicationStartedEvent event) {
        logQueueState("ApplicationStarted", event.getApplicationContext());
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        logQueueState("ApplicationReady", event.getApplicationContext());
    }

    private void logQueueState(String phase, ApplicationContext ctx) {
        try {
            ThreadPoolTaskScheduler tpts = ctx.getBean(ThreadPoolTaskScheduler.class);
            var queue = tpts.getScheduledThreadPoolExecutor().getQueue();
            List<String> items = queue.stream()
                    .map(r -> r instanceof RunnableScheduledFuture<?> f
                            ? f.getClass().getSimpleName() + "(delay=" + f.getDelay(TimeUnit.SECONDS) + "s,periodic=" + f.isPeriodic() + ",done=" + f.isDone() + ",cancelled=" + f.isCancelled() + ")"
                            : r.toString())
                    .toList();
            int holderTasks = ctx.getBeansOfType(ScheduledTaskHolder.class).values().stream()
                    .mapToInt(h -> h.getScheduledTasks().size()).sum();
            log.info("[SCHEDULER] {}: {} tâche(s) @Scheduled, activeThreads={}, queue={}",
                    phase, holderTasks, tpts.getScheduledThreadPoolExecutor().getActiveCount(), items);
        } catch (Exception e) {
            log.warn("[SCHEDULER] {} impossible d'inspecter la file : {}", phase, e.getMessage());
        }
    }
}
