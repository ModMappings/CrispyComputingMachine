package org.modmappings.crispycomputingmachine.cache;

import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Component
public class ChunkCacheExecutionListener implements ChunkListener {

    private final VanillaAndExternalMappingCacheManager mappingCacheManager;

    public ChunkCacheExecutionListener(final VanillaAndExternalMappingCacheManager mappingCacheManager) {
        this.mappingCacheManager = mappingCacheManager;
    }

    public VanillaAndExternalMappingCacheManager getMappingCacheManager() {
        return mappingCacheManager;
    }

    @Override
    public void beforeChunk(final ChunkContext context) {
        getMappingCacheManager().initializeCache();
    }

    @Override
    public void afterChunk(final ChunkContext context) {
        getMappingCacheManager().destroyCache();
    }

    @Override
    public void afterChunkError(final ChunkContext context) {
        getMappingCacheManager().destroyCache();
    }
}
