package com.project.mentalhealth.infrastructure.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated pool for ML analysis so a slow ML service never occupies request threads.
 *
 * <p>Bounded queue with a caller-runs policy: under sustained overload the submitting thread
 * absorbs the work rather than the queue growing without limit. Analysis is already
 * fire-and-forget from the caller's perspective, so the worst case is a slower background
 * task, never a lost analysis.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    public static final String ANALYSIS_EXECUTOR = "analysisExecutor";

    @Bean(name = ANALYSIS_EXECUTOR)
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ml-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
