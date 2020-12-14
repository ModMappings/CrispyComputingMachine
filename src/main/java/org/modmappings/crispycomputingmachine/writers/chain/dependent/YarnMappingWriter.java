package org.modmappings.crispycomputingmachine.writers.chain.dependent;

import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.IntermediaryMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.YarnMappingCacheManager;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class YarnMappingWriter extends AbstractDependentMappingWriter {

    public YarnMappingWriter(final DatabaseClient databaseClient, final IntermediaryMappingCacheManager dependentMappingCacheManager, final YarnMappingCacheManager targetChainCacheManager) {
        super(Constants.YARN_MAPPING_NAME, Constants.YARN_MAPPING_STATE_IN, Constants.YARN_MAPPING_STATE_OUT, true, true, Constants.INTERMEDIARY_MAPPING_NAME, databaseClient, dependentMappingCacheManager, targetChainCacheManager);
    }
}
