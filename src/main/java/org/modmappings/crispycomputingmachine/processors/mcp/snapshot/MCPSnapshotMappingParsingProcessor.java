package org.modmappings.crispycomputingmachine.processors.mcp.snapshot;

import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPSnapshotMappingParsingProcessor extends AbstractMCPMappingParsingProcessor
{
    public MCPSnapshotMappingParsingProcessor(final MCPConfigMappingCacheManager mcpConfigMappingCacheManager)
    {
        super(Constants.MCP_SNAPSHOT_WORKING_DIR, mcpConfigMappingCacheManager);
    }
}
