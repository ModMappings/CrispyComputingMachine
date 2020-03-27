package org.modmappings.crispycomputingmachine.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.GameVersionUtils;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.DistributionDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public abstract class AbstractInitialChainElementMappingWriter implements ItemWriter<ExternalMapping> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String mappingName;
    private final String mappingTypeIn;
    private final String mappingTypeOut;
    private final boolean visible;
    private final boolean editable;

    private final DatabaseClient databaseClient;

    private final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager;
    private final AbstractMappingCacheManager targetChainCacheManager;

    protected AbstractInitialChainElementMappingWriter(final String mappingName, final String mappingTypeIn, final String mappingTypeOut, final boolean visible, final boolean editable, final DatabaseClient databaseClient, final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager, final AbstractMappingCacheManager targetChainCacheManager) {
        this.mappingName = mappingName;
        this.mappingTypeIn = mappingTypeIn;
        this.mappingTypeOut = mappingTypeOut;
        this.visible = visible;
        this.editable = editable;
        this.databaseClient = databaseClient;
        this.vanillaAndExternalMappingCacheManager = vanillaAndExternalMappingCacheManager;
        this.targetChainCacheManager = targetChainCacheManager;
    }

    @Override
    public void write(final List<? extends ExternalMapping> items) throws Exception {
        final MappingTypeDMO mappingType = getOrCreateMappingType();
        final Map<Tuple2<UUID, String>, ReleaseDMO> releasesToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, MappingDMO> mappingsToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, ReleaseComponentDMO> releaseComponentsToSave = new LinkedHashMap<>();

        final Set<ReleaseDMO> releasesToUpdate = new HashSet<>();

        if (items.isEmpty())
            return;

        final ExternalMapping first = items.get(0);
        final Map<UUID, VersionedMappableDMO> versionedMappables = getVersionedMappablesForGameVersion(first.getGameVersion());

        items.forEach(evm -> {
            if (!CacheUtils.alreadyExistsOnInput(evm, vanillaAndExternalMappingCacheManager, targetChainCacheManager))
            {
                LOGGER.warn("Could not find a vanilla mapping for: " + evm);
                return;
            }

            final MappableDMO mappable = CacheUtils.getCachedMappableViaInput(evm, vanillaAndExternalMappingCacheManager, targetChainCacheManager);

            GameVersionDMO gameVersion = targetChainCacheManager.getGameVersion(evm.getGameVersion());

            ReleaseDMO release = releasesToSave.containsKey(Tuples.of(mappingType.getId(), evm.getReleaseName())) ?
                    releasesToSave.get(Tuples.of(mappingType.getId(), evm.getReleaseName())) :
                    targetChainCacheManager.getRelease(mappingType.getId(), evm.getReleaseName());

            if (release == null)
            {
                release = new ReleaseDMO(
                        UUID.randomUUID(),
                        Constants.SYSTEM_ID,
                        Timestamp.from(Instant.now()),
                        evm.getGameVersion(),
                        gameVersion.getId(),
                        mappingType.getId(),
                        GameVersionUtils.isPreRelease(evm.getGameVersion()) || GameVersionUtils.isSnapshot(evm.getGameVersion()),
                        "new"
                );

                releasesToSave.put(Tuples.of(mappingType.getId(), evm.getReleaseName()), release);
                targetChainCacheManager.registerNewRelease(release);
            }

            Assert.notNull(release, "Release could not be determined.... How can there be a game version without a release.");

            releasesToUpdate.add(release);

            final UUID vanillaVersionedMappableId = CacheUtils.getInputMappingCacheEntry(
                    evm,
                    vanillaAndExternalMappingCacheManager,
                    targetChainCacheManager
            ).getVersionedMappableId();

            final MappingDMO mapping = new MappingDMO(
                    UUID.randomUUID(),
                    Constants.SYSTEM_ID,
                    Timestamp.from(Instant.now()),
                    vanillaVersionedMappableId,
                    mappingType.getId(),
                    evm.getInput(),
                    evm.getOutput(),
                    "",
                    DistributionDMO.UNKNOWN
            );
            mappingsToSave.put(evm, mapping);

            final ReleaseComponentDMO releaseComponent = new ReleaseComponentDMO(
                    UUID.randomUUID(),
                    release.getId(),
                    mapping.getId()
            );
            releaseComponentsToSave.put(evm, releaseComponent);

            CacheUtils.registerNewEntry(
                    mappable,
                    versionedMappables.get(vanillaVersionedMappableId),
                    mapping,
                    targetChainCacheManager
            );
        });

        if (releasesToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(ReleaseDMO.class)
                    .using(Flux.fromIterable(releasesToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn(String.format("Created: %d new releases from: %d local new instances of: %s mappings", rowsUpdated, releasesToSave.size(), mappingName));
        }

        if (mappingsToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(MappingDMO.class)
                    .using(Flux.fromIterable(mappingsToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn(String.format("Created: %d new %s mappings from: %d local new instances of: %s mappings", rowsUpdated, items.get(0).getMappableType().name().toLowerCase(), mappingsToSave.size(), mappingName));
        }

        if (releaseComponentsToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(ReleaseComponentDMO.class)
                    .using(Flux.fromIterable(releaseComponentsToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn(String.format("Created: %d new %s release component from: %d local new instances of: %s mappings", rowsUpdated, items.get(0).getMappableType().name().toLowerCase(), releaseComponentsToSave.size(), mappingName));
        }

        releasesToUpdate.forEach(release -> {
            release.setState(items.get(0).getMappableType().name().toLowerCase());

            databaseClient.update()
                    .table(ReleaseDMO.class)
                    .using(release)
                    .fetch()
                    .rowsUpdated()
                    .block();
        });
    }

    protected abstract boolean shouldCorrectVanilla();

    private Map<UUID, VersionedMappableDMO> getVersionedMappablesForGameVersion(String version) {
        return databaseClient.execute(String.format("SELECT vc.* from versioned_mappable vc JOIN game_version gv on vc.game_version_id = gv.id WHERE gv.name = '%s'", version))
                .as(VersionedMappableDMO.class)
                .fetch()
                .all()
                .collectMap(VersionedMappableDMO::getId, Function.identity())
                .block();
    }

    private MappingTypeDMO getOrCreateMappingType()
    {
        return databaseClient.select()
                .from(MappingTypeDMO.class)
                .matching(Criteria.where("name").is(mappingName))
                .fetch()
                .first()
                .switchIfEmpty(Mono.just(new MappingTypeDMO(UUID.randomUUID(), Constants.SYSTEM_ID, Timestamp.from(Instant.now()), mappingName, visible, editable, mappingTypeIn, mappingTypeOut))
                        .flatMap(mappingType -> databaseClient.insert()
                                .into(MappingTypeDMO.class)
                                .using(mappingType)
                                .fetch()
                                .first()
                                .map(r -> mappingType))
                ).block();
    }
}
