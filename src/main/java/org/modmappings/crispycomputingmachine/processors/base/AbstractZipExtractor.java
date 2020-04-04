package org.modmappings.crispycomputingmachine.processors.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class AbstractZipExtractor implements ItemProcessor<String, String> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String fileName;
    private final String targetDirectory;
    private final String mappingTypeName;

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    protected AbstractZipExtractor(final String fileName, final String targetDirectory, final String mappingTypeName) {
        this.fileName = fileName;
        this.targetDirectory = targetDirectory;
        this.mappingTypeName = mappingTypeName;
    }

    @Override
    public String process(final String item) throws Exception {
        try {
            final File workingDirectoryFile = workingDirectory.getFile();
            workingDirectoryFile.mkdirs();
            final File versionWorkingDirectory = new File(workingDirectoryFile, item);
            versionWorkingDirectory.mkdirs();
            final File mappingJarFile = new File(versionWorkingDirectory, fileName);
            final File unzippingMappingJarTarget = new File(versionWorkingDirectory, targetDirectory);
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
            LOGGER.warn(String.format("Failed to extract the %s jar for: %s", mappingTypeName, item), e);
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
