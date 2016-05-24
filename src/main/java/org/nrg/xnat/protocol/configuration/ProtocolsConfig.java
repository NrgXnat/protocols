package org.nrg.xnat.protocol.configuration;

import org.nrg.xnat.protocol.tasks.ProtocolScheduledTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.inject.Inject;
import java.util.List;

@Configuration
@EnableScheduling
public class ProtocolsConfig implements SchedulingConfigurer {
    @Bean
    public TriggerTask protocolTask() {
        return new TriggerTask(new ProtocolScheduledTask(), new PeriodicTrigger(900000));
    }

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
        taskRegistrar.addTriggerTask(protocolTask());
//        taskRegistrar.addTriggerTask(disableInactiveUsers());
//        taskRegistrar.addTriggerTask(resetEmailRequests());
//        taskRegistrar.addTriggerTask(clearExpiredAliasTokens());
//        taskRegistrar.addTriggerTask(rebuildSessionXmls());
        for (final TriggerTask triggerTask : _triggerTasks) {
            taskRegistrar.addTriggerTask(triggerTask);
        }
    }

    @Inject
    private List<TriggerTask> _triggerTasks;
}