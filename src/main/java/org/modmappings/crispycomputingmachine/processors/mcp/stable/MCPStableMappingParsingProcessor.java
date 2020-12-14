package org.modmappings.crispycomputingmachine.processors.mcp.stable;

import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.processors.mcp.AbstractMCPMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class MCPStableMappingParsingProcessor extends AbstractMCPMappingParsingProcessor
{
    public MCPStableMappingParsingProcessor(final MCPConfigMappingCacheManager mcpConfigMappingCacheManager)
    {
        super(Constants.MCP_STABLE_WORKING_DIR, mcpConfigMappingCacheManager);
    }
}
