package org.modmappings.crispycomputingmachine.cache;

import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class ChunkCacheExecutionListener implements ChunkListener {

    private final DatabaseClient databaseClient;
    private final MappingCacheManager mappingCacheManager;

    public ChunkCacheExecutionListener(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
        mappingCacheManager = new MappingCacheManager(this.databaseClient);
    }

    public MappingCacheManager getMappingCacheManager() {
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
