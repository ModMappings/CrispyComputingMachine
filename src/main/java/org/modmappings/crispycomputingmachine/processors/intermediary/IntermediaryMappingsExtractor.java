package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.readers.ExternalVanillaMappingReader;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class IntermediaryMappingsExtractor implements ItemProcessor<String, String> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public String process(final String item) throws Exception {
        try {
            final File workingDirectoryFile = workingDirectory.getFile();
            workingDirectoryFile.mkdirs();
            final File versionWorkingDirectory = new File(workingDirectoryFile, item);
            versionWorkingDirectory.mkdirs();
            final File mappingJarFile = new File(versionWorkingDirectory, "intermediary.jar");
            final File unzippingMappingJarTarget = new File(versionWorkingDirectory, "intermediary");
            unzippingMappingJarTarget.mkdirs();

            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(mappingJarFile));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.isDirectory())
                {
                    File destFile = new File(unzippingMappingJarTarget, zipEntry.getName());
                    destFile.mkdirs();
                    zipEntry = zis.getNextEntry();
                }
                else
                {
                    File newFile = newFile(unzippingMappingJarTarget, zipEntry);
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    zipEntry = zis.getNextEntry();
                }
            }
            zis.closeEntry();
            zis.close();


            return item;
        } catch (Exception e) {
            LOGGER.warn("Failed to extract the intermediary jar for: " + item, e);
            return null;
        }
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}