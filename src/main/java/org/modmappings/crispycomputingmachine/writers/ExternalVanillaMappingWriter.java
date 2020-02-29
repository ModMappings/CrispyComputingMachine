package org.modmappings.crispycomputingmachine.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.MappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExternalVanillaMappingWriter implements ItemWriter<ExternalVanillaMapping> {

    private static final Logger LOGGER = LogManager.getLogger(ExternalVanillaMappingWriter.class);

    private final DatabaseClient databaseClient;
    private final ReactiveDataAccessStrategy dataAccessStrategy;
    private final MappingCacheManager mappingCacheManager;

    public ExternalVanillaMappingWriter(final DatabaseClient databaseClient, final ReactiveDataAccessStrategy dataAccessStrategy, final MappingCacheManager mappingCacheManager) {
        this.databaseClient = databaseClient;
        this.dataAccessStrategy = dataAccessStrategy;
        this.mappingCacheManager = mappingCacheManager;
    }

    @Override
    public void write(final List<? extends ExternalVanillaMapping> items) throws Exception {
        final Map<ExternalVanillaMapping, MappableDMO> mappablesToSave = items.parallelStream()
                .filter(evm -> !CacheUtils.vanillaAlreadyExists(evm, mappingCacheManager))
                .collect(Collectors.toMap(Function.identity(), evm -> new MappableDMO(
                            UUID.randomUUID(),
                            Constants.SYSTEM_ID,
                            Timestamp.from(Instant.now()),
                            MappableTypeDMO.valueOf(evm.getMappableType().name())
                        )
                    )
                );


        final int rowsUpdated = databaseClient.insert()
                .into(MappableDMO.class)
                .using(Flux.fromIterable(mappablesToSave.values()))
                .fetch()
                .rowsUpdated()
                .block();

        LOGGER.warn("Created ");

    }
}
