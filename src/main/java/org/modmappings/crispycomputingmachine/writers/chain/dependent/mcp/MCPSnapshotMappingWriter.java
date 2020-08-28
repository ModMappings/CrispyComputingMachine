package org.modmappings.crispycomputingmachine.writers.chain.dependent.mcp;

import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MCPMappingCacheManager;
import org.modmappings.crispycomputingmachine.writers.chain.dependent.AbstractMCPMappingWriter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class MCPSnapshotMappingWriter extends AbstractMCPMappingWriter
{
    protected MCPSnapshotMappingWriter(
      final DatabaseClient databaseClient,
      final MCPConfigMappingCacheManager dependentMappingCacheManager,
      final MCPMappingCacheManager targetChainCacheManager)
    {
        super(databaseClient, dependentMappingCacheManager, targetChainCacheManager, true);
    }
}
