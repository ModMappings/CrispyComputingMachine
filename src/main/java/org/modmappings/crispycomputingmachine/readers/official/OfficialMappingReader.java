package org.modmappings.crispycomputingmachine.readers.official;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import net.minecraftforge.lex.mappingtoy.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.LauncherMetadata;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Component
public class OfficialMappingReader extends AbstractItemStreamItemReader<ExternalVanillaMapping> implements InitializingBean, PeekableItemReader<ExternalVanillaMapping> {

    private static final Logger LOGGER = LogManager.getLogger(OfficialMappingReader.class);

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private final CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> internalVanillaMappingReaderProcessor;

    private PeekingIterator<VersionsItem> versionIterator = null;
    private VersionsItem currentVersion = null;
    private PeekingIterator<ExternalVanillaMapping> vanillaMappingIterator = null;

    public OfficialMappingReader(final CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> internalVanillaMappingReaderProcessor) {
        this.internalVanillaMappingReaderProcessor = internalVanillaMappingReaderProcessor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectory, "Working directory needs to be set.");
        Assert.notNull(internalVanillaMappingReaderProcessor, "The internal processor needs to be set.");
    }

    @Override
    public void open(final ExecutionContext executionContext) {
        LOGGER.info("Starting the download of the Minecraft Version.");
        super.open(executionContext);
        try {
            final File workingDir = workingDirectory.getFile();
            Assert.state(workingDir.exists(), "Working directory does not exist: " + workingDir.getAbsolutePath());
            Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());
            File manifestFile = new File(workingDir, Constants.MANIFEST_WORKING_FILE);
            Assert.state(manifestFile.exists(), "Minecraft version manifest file does not exist!");
            final LauncherMetadata manifestJson = Utils.loadJson(manifestFile.toPath(), LauncherMetadata.class);
            this.versionIterator = Iterators.peekingIterator(manifestJson.getVersions().iterator());
            LOGGER.info("Downloaded the Minecraft Launcher Manifest to: " + manifestFile.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load manifest json.", e);
        }
    }

    @Override
    public ExternalVanillaMapping read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Assert.notNull(this.versionIterator, "Missing the version iterator");
        while (this.versionIterator.hasNext() && (this.currentVersion == null || this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext())) {
            this.currentVersion = this.versionIterator.next();
            LOGGER.info("Loaded: " + this.currentVersion.toString() + " MC Version.");

            if (this.currentVersion != null && (this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext()))
            {
                final List<ExternalVanillaMapping> currentVersionMappings = this.internalVanillaMappingReaderProcessor.process(this.currentVersion);
                if (currentVersionMappings != null)
                    this.vanillaMappingIterator = Iterators.peekingIterator(Objects.requireNonNull(currentVersionMappings).iterator());
                else
                    this.vanillaMappingIterator = null;
            }
        }

        if (this.vanillaMappingIterator != null)
        {
            return this.vanillaMappingIterator.next();
        }

        LOGGER.info("Loaded all MC Versions.");
        return null;
    }

    @Override
    public ExternalVanillaMapping peek() throws Exception, UnexpectedInputException, ParseException {
        Assert.notNull(this.versionIterator, "Missing the version iterator");
        while (this.versionIterator.hasNext() && (this.currentVersion == null || this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext())) {
            this.currentVersion = this.versionIterator.next();
            if (this.currentVersion != null && (this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext()))
            {
                final List<ExternalVanillaMapping> currentVersionMappings = this.internalVanillaMappingReaderProcessor.process(this.currentVersion);
                if (currentVersionMappings != null)
                    this.vanillaMappingIterator = Iterators.peekingIterator(Objects.requireNonNull(currentVersionMappings).iterator());
                else
                    this.vanillaMappingIterator = null;
            }
        }

        if (this.vanillaMappingIterator != null)
        {
            return this.vanillaMappingIterator.peek();
        }

        return null;
    }
}
