package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalRelease;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.processors.intermediary.*;
import org.modmappings.crispycomputingmachine.processors.mcpconfig.*;
import org.modmappings.crispycomputingmachine.processors.release.ExternalReleaseToExternalVanillaMappingProcessor;
import org.modmappings.crispycomputingmachine.processors.release.ExternalVanillaMappingSorter;
import org.modmappings.crispycomputingmachine.processors.official.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ProcessorConfiguration {

    @Bean
    public CompositeItemProcessor<String, List<ExternalMapping>> internalMCPConfigMappingReaderProcessor(
            final ConfigurationMCPConfigMappingMinecraftVersionFilter configurationMCPConfigMappingMinecraftVersionFilter,
            final ExistingMCPConfigMappingMinecraftVersionFilter existingMCPConfigMappingMinecraftVersionFilter,
            final MCPConfigMappingsDownloader mCPConfigMappingsDownloader,
            final MCPConfigMappingFileExtractor fileExtractor,
            final MCPConfigMappingsExtractor mappingsExtractor
    ) {
        final CompositeItemProcessor<String, List<ExternalMapping>> compositeItemProcessor = new CompositeItemProcessor<>();
        final ArrayList<ItemProcessor<?,?>> processors = new ArrayList<>();

        processors.add(configurationMCPConfigMappingMinecraftVersionFilter);
        processors.add(existingMCPConfigMappingMinecraftVersionFilter);
        processors.add(mCPConfigMappingsDownloader);
        processors.add(mappingsExtractor);
        processors.add(fileExtractor);
        compositeItemProcessor.setDelegates(processors);

        return compositeItemProcessor;
    }
    
    @Bean
    public CompositeItemProcessor<String, List<ExternalMapping>> internalIntermediaryMappingReaderProcessor(
            final IntermediaryConfigGameVersionFilter configurationIntermediaryMappingMinecraftVersionFilter,
            final IntermediarySkipIfOfficialNotReadyFilter existingIntermediaryMappingMinecraftVersionFilter,
            final IntermediarySkipIfReleaseExistsFilter skipIfReleaseExistsFilter,
            final IntermediaryDownloadingProcessor intermediaryMappingsDownloader,
            final IntermediaryMappingFileExtractor fileExtractor,
            final IntermediaryMappingsExtractor mappingsExtractor
    ) {
        final CompositeItemProcessor<String, List<ExternalMapping>> compositeItemProcessor = new CompositeItemProcessor<>();
        final ArrayList<ItemProcessor<?,?>> processors = new ArrayList<>();

        processors.add(configurationIntermediaryMappingMinecraftVersionFilter);
        processors.add(existingIntermediaryMappingMinecraftVersionFilter);
        processors.add(skipIfReleaseExistsFilter);
        processors.add(intermediaryMappingsDownloader);
        processors.add(mappingsExtractor);
        processors.add(fileExtractor);
        compositeItemProcessor.setDelegates(processors);

        return compositeItemProcessor;
    }

    @Bean
    public CompositeItemProcessor<VersionsItem, ExternalRelease> performMinecraftVersionImportProcessor(
            final OfficialMappingPublishedVersionFilter officialMappingPublishedVersionFilter,
            final ConfigurationBasedOfficialMappingMinecraftVersionFilter configurationBasedMinecraftVersionFilter,
            final ExistingOfficialMappingMinecraftVersionFilter existingMinecraftVersionFilter,
            final MappingToyInformationExtractor mappingToyInformationExtractor,
            final MTToMMInfoConverter mtToMMInfoConverter
    ) {
        final CompositeItemProcessor<VersionsItem, ExternalRelease> compositeItemProcessor = new CompositeItemProcessor<>();
        final ArrayList<ItemProcessor<?,?>> processors = new ArrayList<>();

        processors.add(officialMappingPublishedVersionFilter);
        processors.add(configurationBasedMinecraftVersionFilter);
        processors.add(existingMinecraftVersionFilter);
        processors.add(mappingToyInformationExtractor);
        processors.add(mtToMMInfoConverter);
        compositeItemProcessor.setDelegates(processors);

        return compositeItemProcessor;
    }

    @Bean
    public CompositeItemProcessor<ExternalRelease, List<ExternalVanillaMapping>> releaseToVanillaMappingsConverter(
            final ExternalReleaseToExternalVanillaMappingProcessor externalReleaseToExternalVanillaMappingProcessor,
            final ExternalVanillaMappingSorter externalVanillaMappingSorter
    )
    {
        final CompositeItemProcessor<ExternalRelease, List<ExternalVanillaMapping>> compositeItemProcessor = new CompositeItemProcessor<>();
        final ArrayList<ItemProcessor<?,?>> processors = new ArrayList<>();

        processors.add(externalReleaseToExternalVanillaMappingProcessor);
        processors.add(externalVanillaMappingSorter);

        compositeItemProcessor.setDelegates(processors);
        return compositeItemProcessor;
    }

    @Bean
    public CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> internalVanillaMappingReaderProcessor(
            final CompositeItemProcessor<VersionsItem, ExternalRelease> performMinecraftVersionImportProcessor,
            final CompositeItemProcessor<ExternalRelease, List<ExternalVanillaMapping>> releaseToVanillaMappingsConverter
    ) {
        final CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> compositeItemProcessor = new CompositeItemProcessor<>();
        final ArrayList<ItemProcessor<?,?>> processors = new ArrayList<>();

        processors.add(performMinecraftVersionImportProcessor);
        processors.add(releaseToVanillaMappingsConverter);

        compositeItemProcessor.setDelegates(processors);
        return compositeItemProcessor;
    }
}
