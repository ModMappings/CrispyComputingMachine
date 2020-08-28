package org.modmappings.crispycomputingmachine.processors.mcp.snapshot;

import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPSnapshotZipExtractionProcessor extends AbstractMCPZipExtractionProcessor
{
    public MCPSnapshotZipExtractionProcessor()
    {
        super(
          Constants.MCP_SNAPSHOT_JAR,
          Constants.MCP_SNAPSHOT_WORKING_DIR
        );
    }
}
