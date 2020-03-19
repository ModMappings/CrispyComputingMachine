package org.modmappings.crispycomputingmachine.cache;

import com.google.common.collect.Lists;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.Collection;

public class ChunkCacheExecutionListener implements ChunkListener {

    private final Collection<AbstractMappingCacheManager> mappingCacheManagers;

    public ChunkCacheExecutionListener(final AbstractMappingCacheManager... mappingCacheManager) {
        this.mappingCacheManagers = Lists.newArrayList(mappingCacheManager);
    }

    public Collection<AbstractMappingCacheManager> getMappingCacheManager() {
        return mappingCacheManagers;
    }

    @Override
    public void beforeChunk(final ChunkContext context) {
        getMappingCacheManager().forEach(AbstractMappingCacheManager::initializeCache);
    }

    @Override
    public void afterChunk(final ChunkContext context) {
        getMappingCacheManager().forEach(AbstractMappingCacheManager::destroyCache);
    }

    @Override
    public void afterChunkError(final ChunkContext context) {
        getMappingCacheManager().forEach(AbstractMappingCacheManager::destroyCache);
    }
}
