package org.modmappings.crispycomputingmachine.cache;

import com.google.common.collect.Lists;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.util.Collection;

public class ChunkCacheExecutionListener implements ChunkListener {

    private final AbstractSimpleBasedCacheManager             remappingManager;
    private final Collection<AbstractSimpleBasedCacheManager> mappingCacheManagers;

    public ChunkCacheExecutionListener(final AbstractSimpleBasedCacheManager remappingManager, final AbstractSimpleBasedCacheManager... mappingCacheManager) {
        this.remappingManager = remappingManager;
        this.mappingCacheManagers = Lists.newArrayList(mappingCacheManager);
    }

    public AbstractSimpleBasedCacheManager getRemappingManager() {
        return remappingManager;
    }

    public Collection<AbstractSimpleBasedCacheManager> getMappingCacheManagers() {
        return mappingCacheManagers;
    }

    @Override
    public void beforeChunk(final ChunkContext context) {
    }

    @Override
    public void afterChunk(final ChunkContext context) {
        getRemappingManager().destroyCache();
        getMappingCacheManagers().forEach(AbstractSimpleBasedCacheManager::destroyCache);
    }

    @Override
    public void afterChunkError(final ChunkContext context) {
        getRemappingManager().destroyCache();
        getMappingCacheManagers().forEach(AbstractSimpleBasedCacheManager::destroyCache);
    }
}
