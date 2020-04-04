package org.modmappings.crispycomputingmachine.processors.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

public abstract class AbstractDownloadingProcessor implements ItemProcessor<String, String> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<String, URL> releaseToUrlFunction;
    private final String fileName;
    private final String mappingTypeName;

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    public AbstractDownloadingProcessor(final Function<String, URL> releaseToUrlFunction, final String fileName, final String mappingTypeName) {
        this.releaseToUrlFunction = releaseToUrlFunction;
        this.fileName = fileName;
        this.mappingTypeName = mappingTypeName;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public String process(@NotNull final String item) throws Exception {
        try {
            final File workingDirectoryFile = workingDirectory.getFile();
            workingDirectoryFile.mkdirs();
            final File versionWorkingDirectory = new File(workingDirectoryFile, item);
            versionWorkingDirectory.mkdirs();
            final File mappingJarFile = new File(versionWorkingDirectory, fileName);

            final URL mappingJarURL = releaseToUrlFunction.apply(item); //new URL(Constants.INTERMEDIARY_MAPPING_REPO + item + "/intermediary-" + item + ".jar");
            final InputStream in = mappingJarURL.openStream();
            Files.copy(in, mappingJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return item;
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to download the %s jar for: %s", mappingTypeName, item), e);
            return null;
        }
    }
}
