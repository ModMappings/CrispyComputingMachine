package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.processors.base.AbstractZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
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
public class MCPConfigZipExtractionProcessor extends AbstractZipExtractionProcessor {

    protected MCPConfigZipExtractionProcessor() {
        super(Constants.MCP_CONFIG_ZIP,
                Constants.MCP_CONFIG_WORKING_DIR,
                Constants.MCP_CONFIG_MAPPING_NAME);
    }
}
