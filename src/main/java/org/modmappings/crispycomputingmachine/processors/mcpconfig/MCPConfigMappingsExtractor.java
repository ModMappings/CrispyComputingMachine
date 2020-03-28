package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class MCPConfigMappingsExtractor implements ItemProcessor<String, String> {
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
            final File mappingJarFile = new File(versionWorkingDirectory, "mcp-config.zip");
            final File unzippingMappingJarTarget = new File(versionWorkingDirectory, "mcp-config");
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
            LOGGER.warn("Failed to extract the mcp-config jar for: " + item, e);
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
