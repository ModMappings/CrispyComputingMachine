package org.modmappings.crispycomputingmachine.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;

    public JobConfiguration(final JobBuilderFactory jobBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
    }

    @Bean
    public Job importMinecraftVersionsJob(
            final Step downloadManifestVersion,
            final Step performMinecraftVersionImport
    )
    {
        return jobBuilderFactory.get("importMinecraftVersionsJob")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                .start(downloadManifestVersion)
                .next(performMinecraftVersionImport)
                .build();
    }

    @Bean
    public Job importIntermediaryJob(
            final Step downloadIntermediaryMavenMetadataVersion,
            final Step performIntermediaryImport
    )
    {
        return jobBuilderFactory.get("importIntermediaryJob")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                .start(downloadIntermediaryMavenMetadataVersion)
                .next(performIntermediaryImport)
                .build();
    }

    @Bean
    public Job importYarnJob(
            final Step downloadYarnMavenMetadataVersion,
            final Step performYarnImport
    )
    {
        return jobBuilderFactory.get("importYarnJob")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                .start(downloadYarnMavenMetadataVersion)
                .next(performYarnImport)
                .build();
    }

    @Bean
    public Job importMCPConfigJob(
      final Step downloadMCPConfigMavenMetadataVersion,
      final Step performMCPConfigImport
    )
    {
        return jobBuilderFactory.get("importMCPConfigJob")
                 .preventRestart()
                 .incrementer(new RunIdIncrementer())
                 .start(downloadMCPConfigMavenMetadataVersion)
                 .next(performMCPConfigImport)
                 .build();
    }

    @Bean
    public Job importMCPSnapshotJob(
      final Step downloadMCPSnapshotMavenMetadataVersion,
      final Step performMCPSnapshotImport
    )
    {
        return jobBuilderFactory.get("importMCPSnapshotJob")
                 .preventRestart()
                 .incrementer(new RunIdIncrementer())
                 .start(downloadMCPSnapshotMavenMetadataVersion)
                 .next(performMCPSnapshotImport)
                 .build();
    }

    @Bean
    public Job importMCPStableJob(
      final Step downloadMCPStableMavenMetadataVersion,
      final Step performMCPStableImport
    )
    {
        return jobBuilderFactory.get("importMCPStableJob")
                 .preventRestart()
                 .incrementer(new RunIdIncrementer())
                 .start(downloadMCPStableMavenMetadataVersion)
                 .next(performMCPStableImport)
                 .build();
    }


}
