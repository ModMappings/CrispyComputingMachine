package org.modmappings.crispycomputingmachine.cache;

import com.google.common.collect.Lists;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.util.Collection;

public class ChunkCacheExecutionListener implements ChunkListener {

    private final AbstractMappingCacheManager remappingManager;
    private final Collection<AbstractMappingCacheManager> mappingCacheManagers;

    public ChunkCacheExecutionListener(final AbstractMappingCacheManager remappingManager, final AbstractMappingCacheManager... mappingCacheManager) {
        this.remappingManager = remappingManager;
        this.mappingCacheManagers = Lists.newArrayList(mappingCacheManager);
    }

    public AbstractMappingCacheManager getRemappingManager() {
        return remappingManager;
    }

    public Collection<AbstractMappingCacheManager> getMappingCacheManagers() {
        return mappingCacheManagers;
    }

    @Override
    public void beforeChunk(final ChunkContext context) {
        getRemappingManager().initializeCache();

        getMappingCacheManagers().forEach(c -> c.setRemappingManager(getRemappingManager()));
        getMappingCacheManagers().forEach(AbstractMappingCacheManager::initializeCache);
    }

    @Override
    public void afterChunk(final ChunkContext context) {
        getRemappingManager().destroyCache();
        getMappingCacheManagers().forEach(AbstractMappingCacheManager::destroyCache);
    }

    @Override
    public void afterChunkError(final ChunkContext context) {
        getRemappingManager().destroyCache();
        getMappingCacheManagers().forEach(AbstractMappingCacheManager::destroyCache);
    }
}
