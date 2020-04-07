package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.cache.*;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.readers.others.IntermediaryMappingReader;
import org.modmappings.crispycomputingmachine.readers.official.OfficialMappingReader;
import org.modmappings.crispycomputingmachine.readers.others.MCPConfigMappingReader;
import org.modmappings.crispycomputingmachine.readers.others.YarnMappingReader;
import org.modmappings.crispycomputingmachine.readers.policies.completion.MTRespectingReaderAndCompletionPolicy;
import org.modmappings.crispycomputingmachine.tasks.DownloadIntermediaryManifestTasklet;
import org.modmappings.crispycomputingmachine.tasks.DownloadMCPConfigManifestTasklet;
import org.modmappings.crispycomputingmachine.tasks.DownloadMinecraftManifestTasklet;
import org.modmappings.crispycomputingmachine.tasks.DownloadYarnManifestTasklet;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.writers.OfficialMappingWriter;
import org.modmappings.crispycomputingmachine.writers.chain.dependent.YarnMappingWriter;
import org.modmappings.crispycomputingmachine.writers.chain.initial.IntermediaryMappingWriter;
import org.modmappings.crispycomputingmachine.writers.chain.initial.MCPConfigMappingWriter;
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
    public Step performMinecraftVersionImport(
            final OfficialMappingReader reader,
            final OfficialMappingWriter writer,
            final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager
            )
    {
        final MTRespectingReaderAndCompletionPolicy policyReader = new MTRespectingReaderAndCompletionPolicy(reader);

        return stepBuilderFactory
                .get(Constants.IMPORT_MINECRAFT_VANILLA_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(policyReader)
                .reader(policyReader)
                .writer(writer)
                .listener(new ChunkCacheExecutionListener(vanillaAndExternalMappingCacheManager))
                .build();
    }

    @Bean
    public Step performIntermediaryImport(
            final IntermediaryMappingReader reader,
            final IntermediaryMappingWriter writer,
            final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager,
            final IntermediaryMappingCacheManager intermediaryMappingCacheManager
            )
    {
        final MTRespectingReaderAndCompletionPolicy policyReader = new MTRespectingReaderAndCompletionPolicy(reader);

        return stepBuilderFactory
                .get(Constants.IMPORT_INTERMEDIARY_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(policyReader)
                .reader(policyReader)
                .writer(writer)
                .listener(new ChunkCacheExecutionListener(intermediaryMappingCacheManager, vanillaAndExternalMappingCacheManager))
                .build();
    }

    @Bean
    public Step performMCPConfigImport(
            final MCPConfigMappingReader reader,
            final MCPConfigMappingWriter writer,
            final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager,
            final MCPConfigMappingCacheManager mcpConfigMappingCacheManager
    )
    {
        final MTRespectingReaderAndCompletionPolicy policyReader = new MTRespectingReaderAndCompletionPolicy(reader);

        return stepBuilderFactory
                .get(Constants.IMPORT_MCPCONFIG_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(policyReader)
                .reader(policyReader)
                .writer(writer)
                .listener(new ChunkCacheExecutionListener(mcpConfigMappingCacheManager, vanillaAndExternalMappingCacheManager))
                .build();
    }

    @Bean
    public Step performYarnImport(
            final YarnMappingReader reader,
            final YarnMappingWriter writer,
            final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager,
            final IntermediaryMappingCacheManager intermediaryMappingCacheManager,
            final YarnMappingCacheManager yarnMappingCacheManager
    )
    {
        final MTRespectingReaderAndCompletionPolicy policyReader = new MTRespectingReaderAndCompletionPolicy(reader);

        return stepBuilderFactory
                .get(Constants.IMPORT_YARN_MAPPINGS)
                .<ExternalVanillaMapping, ExternalVanillaMapping>chunk(policyReader)
                .reader(policyReader)
                .writer(writer)
                .listener(new ChunkCacheExecutionListener(vanillaAndExternalMappingCacheManager, intermediaryMappingCacheManager, yarnMappingCacheManager))
                .build();
    }
}
