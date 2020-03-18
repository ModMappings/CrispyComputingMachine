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
}
