package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.cache.ChunkCacheExecutionListener;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingBasedCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.readers.official.OfficialMappingReader;
import org.modmappings.crispycomputingmachine.readers.policies.completion.GameVersionRespectingPolicyReader;
import org.modmappings.crispycomputingmachine.tasks.*;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.writers.OfficialMappingWriter;
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
    public Step downloadIntermediaryMavenMetadataVersion(
            final DownloadIntermediaryManifestTasklet downloadIntermediaryManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_INTERMEDIARY_MAVEN_METADATA_STEP_NAME)
                .tasklet(downloadIntermediaryManifestTasklet)
                .build();
    }

    @Bean
    public Step downloadMCPConfigMavenMetadataVersion(
            final DownloadMCPConfigManifestTasklet downloadMCPConfigManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_MCPCONFIG_MAVEN_METADATA_STEP_NAME)
                .tasklet(downloadMCPConfigManifestTasklet)
                .build();
    }

    @Bean
    public Step downloadYarnMavenMetadataVersion(
      final DownloadYarnManifestTasklet downloadYarnManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_YARN_MAVEN_METADATA_STEP_NAME)
                 .tasklet(downloadYarnManifestTasklet)
                 .build();
    }

    @Bean
    public Step downloadMCPSnapshotMavenMetadataVersion(
      final DownloadMCPSnapshotManifestTasklet downloadMCPSnapshotManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_MCPSNAPSHOT_MAVEN_METADATA_STEP_NAME)
                 .tasklet(downloadMCPSnapshotManifestTasklet)
                 .build();
    }

    @Bean
    public Step downloadMCPStableMavenMetadataVersion(
      final DownloadMCPStableManifestTasklet downloadMCPStableManifestTasklet
    )
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_MCPSTABLE_MAVEN_METADATA_STEP_NAME)
                 .tasklet(downloadMCPStableManifestTasklet)
                 .build();
    }
        
    @Bean
    public Step performMinecraftVersionImport(
            final OfficialMappingReader reader,
            final OfficialMappingWriter writer,
            final VanillaAndExternalMappingBasedCacheManager vanillaAndExternalMappingCacheManager
            )
    {
        final GameVersionRespectingPolicyReader<ExternalVanillaMapping> policyReader = new GameVersionRespectingPolicyReader<>(reader);

        return stepBuilderFactory
                .get(Constants.IMPORT_MINECRAFT_VANILLA_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(policyReader)
                .reader(policyReader)
                .writer(writer)
                .listener(new ChunkCacheExecutionListener(vanillaAndExternalMappingCacheManager))
                .build();
    }
}
