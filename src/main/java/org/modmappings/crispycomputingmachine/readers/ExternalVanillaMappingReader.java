package org.modmappings.crispycomputingmachine.readers;

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
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ExternalVanillaMappingReader extends AbstractItemStreamItemReader<ExternalVanillaMapping> implements InitializingBean {

    private static final Logger LOGGER = LogManager.getLogger(ExternalVanillaMappingReader.class);

    private Resource workingDirectoryResource;
    private LauncherMetadata manifestJson;
    private Iterator<VersionsItem> versionIterator;

    private VersionsItem currentVersion = null;
    private Iterator<ExternalVanillaMapping> vanillaMappingIterator = null;
    private CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> internalProcessor;

    public void setWorkingDirectoryResource(final Resource workingDirectoryResource) {
        this.workingDirectoryResource = workingDirectoryResource;
    }

    public void setInternalProcessor(final CompositeItemProcessor<VersionsItem, List<ExternalVanillaMapping>> internalProcessor) {
        this.internalProcessor = internalProcessor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectoryResource, "Working directory needs to be set.");
        Assert.notNull(internalProcessor, "The internal processor needs to be set.");
    }

    @Override
    public void open(final ExecutionContext executionContext) {
        LOGGER.info("Starting the download of the Minecraft Version.");
        super.open(executionContext);
        try {
            final File workingDir = workingDirectoryResource.getFile();
            Assert.state(workingDir.exists(), "Working directory does not exist: " + workingDir.getAbsolutePath());
            Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());
            File manifestFile = new File(workingDir, Constants.MANIFEST_WORKING_FILE);
            Assert.state(manifestFile.exists(), "Minecraft version manifest file does not exist!");
            this.manifestJson = Utils.loadJson(manifestFile.toPath(), LauncherMetadata.class);
            this.versionIterator = this.manifestJson.getVersions().iterator();
            LOGGER.info("Downloaded the Minecraft Launcher Manifest to: " + manifestFile.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load manifest json.", e);
        }
    }

    @Override
    public ExternalVanillaMapping read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Assert.notNull(this.versionIterator, "Missing version iterator");
        if (this.versionIterator.hasNext() && (this.currentVersion == null || this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext())) {
            this.currentVersion = this.versionIterator.next();
            LOGGER.info("Loaded: " + this.currentVersion.toString() + " MC Version.");

            if (this.currentVersion != null && (this.vanillaMappingIterator == null || !this.vanillaMappingIterator.hasNext()))
            {
                final List<ExternalVanillaMapping> currentVersionMappings = this.internalProcessor.process(this.currentVersion);
                this.vanillaMappingIterator = Objects.requireNonNull(currentVersionMappings).iterator();
            }
        }

        if (this.vanillaMappingIterator != null)
        {
            return this.vanillaMappingIterator.next();
        }

        LOGGER.info("Loaded all MC Versions.");
        return null;
    }

    private void ensureNextVersion()
    {

    }
}
