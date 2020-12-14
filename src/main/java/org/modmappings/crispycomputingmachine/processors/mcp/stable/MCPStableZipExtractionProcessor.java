package org.modmappings.crispycomputingmachine.processors.mcp.stable;

import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPStableZipExtractionProcessor extends AbstractMCPZipExtractionProcessor
{
    public MCPStableZipExtractionProcessor()
    {
        super(
          Constants.MCP_STABLE_JAR,
          Constants.MCP_STABLE_WORKING_DIR
        );
    }
}
