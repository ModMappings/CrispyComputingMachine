package org.modmappings.crispycomputingmachine.writers.chain.dependent;

import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MCPMappingCacheManager;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.data.r2dbc.core.DatabaseClient;

public abstract class AbstractMCPMappingWriter extends AbstractDependentMappingWriter
{
    private final boolean isSnapshot;

    protected AbstractMCPMappingWriter(
      final DatabaseClient databaseClient,
      final MCPConfigMappingCacheManager dependentMappingCacheManager,
      final MCPMappingCacheManager targetChainCacheManager,
      final boolean isSnapshot) {
        super(Constants.MCP_MAPPING_NAME, Constants.MCP_MAPPING_STATE_IN, Constants.MCP_MAPPING_STATE_OUT, true, true, Constants.MCP_CONFIG_MAPPING_NAME, databaseClient, dependentMappingCacheManager, targetChainCacheManager);
        this.isSnapshot = isSnapshot;
    }

    @Override
    protected boolean isReleaseSnapshot(final String gameVersion, final String releaseName)
    {
        return isSnapshot;
    }
}
