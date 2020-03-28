package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class MCPConfigMappingsDownloader implements ItemProcessor<String, String> {

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

            final URL mappingJarURL = new URL(Constants.MCP_CONFIG_MAPPING_REPO + item + "/mcp_config-" + item + ".zip");
            final InputStream in = mappingJarURL.openStream();
            Files.copy(in, mappingJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return item;
        } catch (Exception e) {
            LOGGER.warn("Failed to download the intermediary jar for: " + item, e);
            return null;
        }
    }
}
