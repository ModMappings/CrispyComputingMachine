package org.modmappings.crispycomputingmachine.processors.mcp;

import org.modmappings.crispycomputingmachine.processors.base.AbstractZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

public abstract class AbstractMCPZipExtractionProcessor extends AbstractZipExtractionProcessor {

    protected AbstractMCPZipExtractionProcessor(
      final String MCP_FILENAME,
      final String MCP_WORKING_DIR
    ) {
        super(
            MCP_FILENAME,
            MCP_WORKING_DIR,
            Constants.MCP_MAPPING_NAME
        );
    }
}
