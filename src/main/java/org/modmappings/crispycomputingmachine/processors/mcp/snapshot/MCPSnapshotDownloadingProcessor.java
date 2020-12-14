package org.modmappings.crispycomputingmachine.processors.mcp.snapshot;

import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPSnapshotDownloadingProcessor extends AbstractMCPDownloadingProcessor
{

    public MCPSnapshotDownloadingProcessor()
    {
        super(
          Constants.MCP_SNAPSHOT_MAPPING_REPO,
          Constants.MCP_SNAPSHOT_ARTIFACT,
          Constants.MCP_SNAPSHOT_JAR
        );
    }
}
