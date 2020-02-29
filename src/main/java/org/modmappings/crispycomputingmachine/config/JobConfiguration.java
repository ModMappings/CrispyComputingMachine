package org.modmappings.crispycomputingmachine.config;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalRelease;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.processors.*;
import org.modmappings.crispycomputingmachine.processors.version.ExistingMinecraftVersionFilter;
import org.modmappings.crispycomputingmachine.readers.ExternalVanillaMappingReader;
import org.modmappings.crispycomputingmachine.tasks.DeleteWorkingDirectoryTasklet;
import org.modmappings.crispycomputingmachine.tasks.DownloadMinecraftManifestTasklet;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.writers.ModMappingsReleaseWriter;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.components.ReleaseComponentRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.inheritancedata.InheritanceDataRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.mappable.MappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.versionedmappables.VersionedMappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappings.mapping.MappingRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.util.ArrayList;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public JobConfiguration(final JobBuilderFactory jobBuilderFactory, final StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job importMinecraftVersionsJob(
            final Step deleteWorkingDirectory,
            final Step downloadManifestVersion,
            final Step performMinecraftVersionImport
    )
    {
        return jobBuilderFactory.get("importMinecraftVersionsJob")
                .preventRestart()
                .incrementer(new RunIdIncrementer())
                .start(deleteWorkingDirectory)
                .next(downloadManifestVersion)
                .next(performMinecraftVersionImport)
                .build();
    }

    @Bean
    public Step performMinecraftVersionImport(
            final ExternalVanillaMappingReader minecraftVersionFromManifestReader,
            final CompositeItemProcessor<VersionsItem, ExternalRelease> performMinecraftVersionImportProcessor,
            final ModMappingsReleaseWriter modMappingsReleaseWriter
            )
    {
        return stepBuilderFactory
                .get(Constants.DETERMINE_VERSIONS_TO_IMPORT_STEP_NAME)
                .<VersionsItem, ExternalRelease>chunk(Constants.DETERMINE_VERIONS_TO_IMPORT_CHUNK_SIZE)
                .reader(minecraftVersionFromManifestReader)
                .processor(performMinecraftVersionImportProcessor)
                .writer(modMappingsReleaseWriter)
                .build();
    }

    @Bean
    public ExternalVanillaMappingReader minecraftVersionFromManifestItemReader()
    {
        final ExternalVanillaMappingReader reader = new ExternalVanillaMappingReader();
        reader.setWorkingDirectoryResource(new FileSystemResource(Constants.WORKING_DIR));
        return reader;
    }

    @Bean
    public OfficialMappingPublishedVersionFilter officialMappingPublishedVersionFilter()
    {
        return new OfficialMappingPublishedVersionFilter();
    }

    @Bean
    public ExistingMinecraftVersionFilter existingMinecraftVersionFilter(GameVersionRepository repository)
    {
        return new ExistingMinecraftVersionFilter(repository);
    }

    @Bean
    public MappingToyInformationExtractor mappingToyInformationExtractor()
    {
        final MappingToyInformationExtractor informationExtractor = new MappingToyInformationExtractor();
        informationExtractor.setWorkingDirectoryResource(new FileSystemResource(Constants.WORKING_DIR));
        return informationExtractor;
    }

    @Bean
    public MTToMMInfoConverter mtToMMInfoConverter()
    {
        return new MTToMMInfoConverter();
    }

    @Bean
    public CompositeItemProcessor<VersionsItem, ExternalRelease> performMinecraftVersionImportProcessor(
            final OfficialMappingPublishedVersionFilter officialMappingPublishedVersionFilter,
            final ConfigurationBasedMinecraftVersionFilter configurationBasedMinecraftVersionFilter,
            final ExistingMinecraftVersionFilter existingMinecraftVersionFilter,
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
    public ModMappingsReleaseWriter modMappingsReleaseWriter(
            final ReleaseRepository releaseRepository,
            final MappableRepository mappableRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappingRepository mappingRepository,
            final GameVersionRepository gameVersionRepository,
            final MappingTypeRepository mappingTypeRepository,
            final ReleaseComponentRepository releaseComponentRepository,
            final InheritanceDataRepository inheritanceDataRepository
            ) {
        return new ModMappingsReleaseWriter(
                releaseRepository, mappableRepository, versionedMappableRepository, mappingRepository, releaseComponentRepository, inheritanceDataRepository, gameVersionRepository, mappingTypeRepository
        );
    }

    @Bean
    public Step downloadManifestVersion(final DownloadMinecraftManifestTasklet downloadMinecraftManifestTasklet)
    {
        return stepBuilderFactory.get(Constants.DOWNLOAD_MANIFEST_VERSION_STEP_NAME)
                .tasklet(downloadMinecraftManifestTasklet)
                .build();
    }

    @Bean
    public DownloadMinecraftManifestTasklet downloadMinecraftManifestTasklet()
    {
        final DownloadMinecraftManifestTasklet tasklet = new DownloadMinecraftManifestTasklet();
        tasklet.setWorkingDirectoryResource(new FileSystemResource(Constants.WORKING_DIR));
        return tasklet;
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
    public DeleteWorkingDirectoryTasklet deleteWorkingDirectoryTasklet()
    {
        final DeleteWorkingDirectoryTasklet tasklet = new DeleteWorkingDirectoryTasklet();
        tasklet.setWorkingDirectoryResource(new FileSystemResource(Constants.WORKING_DIR));
        return tasklet;
    }
}
