package com.university.timetable_scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * The two background pools. Separate on purpose: a solve is CPU-bound and can run for minutes,
 * a delivery is IO-bound and takes milliseconds. Sharing one pool would leave a finished run's
 * result queued behind the next solve.
 */
@Configuration
public class AsyncConfig {

    public static final String SOLVER_EXECUTOR = "solverExecutor";
    public static final String WEBHOOK_EXECUTOR = "webhookExecutor";

    /**
     * One thread: the solver is single-threaded by design, so a second concurrent solve only halves
     * the CPU each gets — and the budget is wall-clock, so that loss is real.
     *
     * <p>Short queue, abort policy: a caller who cannot be served is told now, rather than getting
     * a 202 that promises a webhook in minutes and delivers in half an hour.
     */
    @Bean(name = SOLVER_EXECUTOR)
    public ThreadPoolTaskExecutor solverExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("solver-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // Blocking shutdown on a ten-minute solve would stall every redeploy, and nothing is
        // committed until the run finishes, so an interrupted job simply did not happen.
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    /**
     * Small pool, generous queue: deliveries are short and bursty, since a finished run emits
     * several at once. Worth draining on shutdown — losing one means a school never hears about a
     * timetable that was generated.
     */
    @Bean(name = WEBHOOK_EXECUTOR)
    public ThreadPoolTaskExecutor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("webhook-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
