package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalRelease;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.readers.ExternalVanillaMappingReader;
import org.modmappings.crispycomputingmachine.tasks.DeleteWorkingDirectoryTasklet;
import org.modmappings.crispycomputingmachine.tasks.DownloadMinecraftManifestTasklet;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.writers.ExternalVanillaMappingWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StepConfiguration {

    private final StepBuilderFactory stepBuilderFactory;

    public StepConfiguration(final StepBuilderFactory stepBuilderFactory) {
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Step downloadManifestVersion(
            final DownloadMinecraftManifestTasklet downloadMinecraftManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_MANIFEST_VERSION_STEP_NAME)
                .tasklet(downloadMinecraftManifestTasklet)
                .build();
    }

    @Bean
    public Step deleteWorkingDirectory(
            final DeleteWorkingDirectoryTasklet deleteWorkingDirectoryTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DELETE_WORKING_DIR_STEP)
                .tasklet(deleteWorkingDirectoryTasklet)
                .build();
    }

    @Bean
    public Step performMinecraftVersionImport(
            final ExternalVanillaMappingReader reader,
            final ExternalVanillaMappingWriter writer
    )
    {
        return stepBuilderFactory
                .get(Constants.IMPORT_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(Constants.IMPORT_MAPPINGS_CHUNK_SIZE)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
