package org.modmappings.crispycomputingmachine.readers;

import net.minecraftforge.lex.mappingtoy.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.LauncherMetadata;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Iterator;

public class MinecraftVersionFromManifestReader extends AbstractItemStreamItemReader<VersionsItem> implements InitializingBean {

    private static final Logger LOGGER = LogManager.getLogger(MinecraftVersionFromManifestReader.class);

    private Resource workingDirectoryResource;
    private LauncherMetadata manifestJson;
    private Iterator<VersionsItem> versionIterator;

    public void setWorkingDirectoryResource(final Resource workingDirectoryResource) {
        this.workingDirectoryResource = workingDirectoryResource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectoryResource, "Working directory needs to be set.");
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
    public VersionsItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Assert.notNull(this.versionIterator, "Missing version iterator");
        if (this.versionIterator.hasNext()) {
            final VersionsItem version = this.versionIterator.next();
            LOGGER.info("Loaded: " + version.toString() + " MC Version.");
            return version;
        }

        LOGGER.info("Loaded all MC Versions.");
        return null;
    }
}
