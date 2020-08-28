package org.modmappings.crispycomputingmachine.processors.mcp.stable;

import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPStableDownloadingProcessor extends AbstractMCPDownloadingProcessor
{

    public MCPStableDownloadingProcessor()
    {
        super(
          Constants.MCP_STABLE_MAPPING_REPO,
          Constants.MCP_STABLE_ARTIFACT,
          Constants.MCP_STABLE_JAR
        );
    }
}
