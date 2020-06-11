package org.modmappings.crispycomputingmachine.writers.chain.initial;

import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class MCPConfigMappingWriter extends AbstractInitialChainElementMappingWriter {

    @Value("${importer.mcpconfig.correcting.vanilla:true}")
    boolean correctsVanilla;

    public MCPConfigMappingWriter(final DatabaseClient databaseClient, final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager, final MCPConfigMappingCacheManager targetChainCacheManager) {
        super(
                Constants.MCP_CONFIG_MAPPING_NAME,
                Constants.MCP_CONFIG_MAPPING_STATE_IN,
                Constants.MCP_CONFIG_MAPPING_STATE_OUT,
                true,
                false,
                databaseClient,
                vanillaAndExternalMappingCacheManager,
                targetChainCacheManager
        );
    }

    @Override
    protected boolean shouldCorrectVanilla() {
        return correctsVanilla;
    }
}
