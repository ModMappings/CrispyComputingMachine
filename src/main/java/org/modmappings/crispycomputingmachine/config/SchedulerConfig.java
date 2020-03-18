package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.quartz.CCMQuartzJob;
import org.quartz.*;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.io.IOException;
import java.util.Properties;
import java.util.TimeZone;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    private final JobLauncher jobLauncher;
    private final JobLocator jobLocator;

    @Value("${importer.vanilla.schedule:0 */1 * * * ?}")
    String vanillaSchedule;

    @Value("${importer.intermediary.schedule:15 */1 * * * ?}")
    String intermediarySchedule;

    public SchedulerConfig(JobLauncher jobLauncher, final JobLocator jobLocator) {
        this.jobLauncher = jobLauncher;
        this.jobLocator = jobLocator;
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
        return jobRegistryBeanPostProcessor;
    }


    @Bean
    public JobDetail importMinecraftVersionsJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importMinecraftVersionsJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                .withIdentity("importMinecraftVersionsJob")
                .setJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger importMinecraftVersionsJobTrigger(
            final JobDetail importMinecraftVersionsJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(vanillaSchedule)
                .withMisfireHandlingInstructionIgnoreMisfires()
                .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                .newTrigger()
                .forJob(importMinecraftVersionsJobDetail)
                .withIdentity("importMinecraftVersionsJobTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail importIntermediaryJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importIntermediaryJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                .withIdentity("importIntermediaryJob")
                .setJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger importIntermediaryJobTrigger(
            final JobDetail importIntermediaryJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(intermediarySchedule)
                .withMisfireHandlingInstructionIgnoreMisfires()
                .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                .newTrigger()
                .forJob(importIntermediaryJobDetail)
                .withIdentity("importIntermediaryJobTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public SchedulerFactoryBean importMinecraftScheduler(
            final JobDetail importMinecraftVersionsJobDetail,
            final Trigger importMinecraftVersionsJobTrigger,
            final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importMinecraftVersionsJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importMinecraftVersionsJobDetail);
        return scheduler;
    }


    @Bean
    public SchedulerFactoryBean importIntermediaryScheduler(
            final JobDetail importIntermediaryJobDetail,
            final Trigger importIntermediaryJobTrigger,
            final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importIntermediaryJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importIntermediaryJobDetail);
        return scheduler;
    }

    @Bean
    public Properties quartzProperties() throws IOException
    {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/application.properties"));
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }
}
