package org.modmappings.crispycomputingmachine.writers.chain.initial;

import org.modmappings.crispycomputingmachine.cache.IntermediaryMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class IntermediaryMappingWriter extends AbstractInitialChainElementMappingWriter {

    @Value("${importer.intermediary.correcting.vanilla:false}")
    boolean correctsVanilla;

    public IntermediaryMappingWriter(final DatabaseClient databaseClient, final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager, final IntermediaryMappingCacheManager targetChainCacheManager) {
        super(
                Constants.INTERMEDIARY_MAPPING_NAME,
                Constants.INTERMEDIARY_MAPPING_STATE_IN,
                Constants.INTERMEDIARY_MAPPING_STATE_OUT,
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
