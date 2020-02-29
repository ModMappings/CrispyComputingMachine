package org.modmappings.crispycomputingmachine.cache;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.r2dbc.core.DatabaseClient;

public class JobCacheExecutionListener implements JobExecutionListener {

    private final DatabaseClient databaseClient;

    private final MappingCacheManager mappingCacheManager;

    public JobCacheExecutionListener(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
        mappingCacheManager = new MappingCacheManager(this.databaseClient);
    }


    @Override
    public void beforeJob(final JobExecution jobExecution) {
        getMappingCacheManager().initializeCache();
    }

    @Override
    public void afterJob(final JobExecution jobExecution) {
        getMappingCacheManager().destroyCache();
    }

    public MappingCacheManager getMappingCacheManager() {
        return mappingCacheManager;
    }
}
