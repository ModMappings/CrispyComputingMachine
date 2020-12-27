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

    @Value("${importer.files.forced:file:none-existing-default-forced-file.jar}")
    Resource forcedFile;

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

            URL mappingJarURL = releaseToUrlFunction.apply(item);

            if (forcedFile.exists() && forcedFile.isReadable()) {
                final File forcedTargetFile = forcedFile.getFile();
                if (forcedTargetFile.getName().replace(".jar", "").equals(item)) {
                    LOGGER.warn("Found forced file: " + forcedTargetFile.getAbsolutePath() + " for release item: " + item + " overriding maven entry with file.");
                    mappingJarURL = forcedTargetFile.toURI().toURL();
                }
            }

            final InputStream in = mappingJarURL.openStream();
            Files.copy(in, mappingJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return item;
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to download the %s jar for: %s", mappingTypeName, item), e);
            return null;
        }
    }
}
