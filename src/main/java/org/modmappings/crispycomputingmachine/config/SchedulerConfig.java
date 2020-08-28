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

    @Value("${importer.mcpconfig.schedule:15 */1 * * * ?}")
    String mcpConfigSchedule;

    @Value("${importer.yarn.schedule:30 */1 * * * ?}")
    String yarnSchedule;
    
    @Value("${importer.mcpsnapshot.schedule:15 */1 * * * ?}")
    String mcpSnapshotSchedule;
    
    @Value("${importer.mcpstable.schedule:15 */1 * * * ?}")
    String mcpStableSchedule;


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
    public JobDetail importYarnJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importYarnJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                .withIdentity("importYarnJob")
                .setJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger importYarnJobTrigger(
            final JobDetail importYarnJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(yarnSchedule)
                .withMisfireHandlingInstructionIgnoreMisfires()
                .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                .newTrigger()
                .forJob(importYarnJobDetail)
                .withIdentity("importYarnJobTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail importMCPConfigJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importMCPConfigJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                .withIdentity("importMCPConfigJob")
                .setJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger importMCPConfigJobTrigger(
            final JobDetail importMCPConfigJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mcpConfigSchedule)
                .withMisfireHandlingInstructionIgnoreMisfires()
                .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                .newTrigger()
                .forJob(importMCPConfigJobDetail)
                .withIdentity("importMCPConfigJobTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail importMCPSnapshotJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importMCPSnapshotJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                 .withIdentity("importMCPSnapshotJob")
                 .setJobData(jobDataMap)
                 .storeDurably()
                 .build();
    }

    @Bean
    public Trigger importMCPSnapshotJobTrigger(
      final JobDetail importMCPSnapshotJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mcpSnapshotSchedule)
                                                          .withMisfireHandlingInstructionIgnoreMisfires()
                                                          .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                 .newTrigger()
                 .forJob(importMCPSnapshotJobDetail)
                 .withIdentity("importMCPSnapshotJobTrigger")
                 .withSchedule(cronScheduleBuilder)
                 .build();
    }

    @Bean
    public JobDetail importMCPStableJobDetail() {
        //Set Job data map
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobName", "importMCPStableJob");
        jobDataMap.put("jobLauncher", jobLauncher);
        jobDataMap.put("jobLocator", jobLocator);

        return JobBuilder.newJob(CCMQuartzJob.class)
                 .withIdentity("importMCPStableJob")
                 .setJobData(jobDataMap)
                 .storeDurably()
                 .build();
    }

    @Bean
    public Trigger importMCPStableJobTrigger(
      final JobDetail importMCPStableJobDetail
    )
    {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mcpStableSchedule)
                                                          .withMisfireHandlingInstructionIgnoreMisfires()
                                                          .inTimeZone(TimeZone.getDefault());

        return TriggerBuilder
                 .newTrigger()
                 .forJob(importMCPStableJobDetail)
                 .withIdentity("importMCPStableJobTrigger")
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
    public SchedulerFactoryBean importYarnScheduler(
            final JobDetail importYarnJobDetail,
            final Trigger importYarnJobTrigger,
            final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importYarnJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importYarnJobDetail);
        return scheduler;
    }


    @Bean
    public SchedulerFactoryBean importMCPConfigScheduler(
            final JobDetail importMCPConfigJobDetail,
            final Trigger importMCPConfigJobTrigger,
            final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importMCPConfigJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importMCPConfigJobDetail);
        return scheduler;
    }
    
    @Bean
    public SchedulerFactoryBean importMCPSnapshotScheduler(
      final JobDetail importMCPSnapshotJobDetail,
      final Trigger importMCPSnapshotJobTrigger,
      final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importMCPSnapshotJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importMCPSnapshotJobDetail);
        return scheduler;
    }

    @Bean
    public SchedulerFactoryBean importMCPStableScheduler(
      final JobDetail importMCPStableJobDetail,
      final Trigger importMCPStableJobTrigger,
      final Properties quartzProperties
    ) throws IOException
    {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
        scheduler.setTriggers(importMCPStableJobTrigger);
        scheduler.setQuartzProperties(quartzProperties);
        scheduler.setJobDetails(importMCPStableJobDetail);
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
