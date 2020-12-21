package org.modmappings.crispycomputingmachine.writers.chain.dependent;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.GameVersionUtils;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.*;
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

public abstract class AbstractDependentMappingWriter  implements ItemWriter<ExternalMapping> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String mappingName;
    private final String mappingTypeIn;
    private final String mappingTypeOut;
    private final boolean visible;
    private final boolean editable;

    private final String dependentMappingName;

    private final DatabaseClient databaseClient;

    private final AbstractMappingCacheManager dependentMappingCacheManager;
    private final AbstractMappingCacheManager targetChainCacheManager;

    public AbstractDependentMappingWriter(final String mappingName, final String mappingTypeIn, final String mappingTypeOut, final boolean visible, final boolean editable, final String dependentMappingName, final DatabaseClient databaseClient, final AbstractMappingCacheManager dependentMappingCacheManager, final AbstractMappingCacheManager targetChainCacheManager) {
        this.mappingName = mappingName;
        this.mappingTypeIn = mappingTypeIn;
        this.mappingTypeOut = mappingTypeOut;
        this.visible = visible;
        this.editable = editable;
        this.dependentMappingName = dependentMappingName;
        this.databaseClient = databaseClient;
        this.dependentMappingCacheManager = dependentMappingCacheManager;
        this.targetChainCacheManager = targetChainCacheManager;
    }

    @Override
    public void write(final List<? extends ExternalMapping> mappings) throws Exception {
        final MappingTypeDMO mappingType = getOrCreateMappingType();
        final Map<ExternalMapping, MappableDMO> mappablesToSave = new LinkedHashMap<>();
        final Map<Tuple2<UUID, String>, ReleaseDMO> releasesToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, MappingDMO> mappingsToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, ProtectedMappableInformationDMO> protectedMappablesToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, ReleaseComponentDMO> releaseComponentsToSave = new LinkedHashMap<>();
        final List<VersionedMappableDMO> versionedMappablesToSave = Lists.newArrayList();

        final Set<ReleaseDMO> releasesToUpdate = new HashSet<>();
        if (mappings.isEmpty())
            return;

        final ExternalMapping first = mappings.get(0);
        final Map<UUID, VersionedMappableDMO> versionedMappables = getVersionedMappablesForGameVersion(first.getGameVersion());

        mappings.forEach(evm -> {
            if (!CacheUtils.alreadyExistsOnOutputFromInput(evm, dependentMappingCacheManager, targetChainCacheManager))
            {
                LOGGER.warn(String.format("Could not find a %s mapping for %s external mapping: %s", dependentMappingName, mappingName, evm));
                return;
            }

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
                        evm.getReleaseName(),
                        gameVersion.getId(),
                        mappingType.getId(),
                        this.isReleaseSnapshot(evm.getGameVersion(), evm.getReleaseName()),
                        "new"
                );

                releasesToSave.put(Tuples.of(mappingType.getId(), evm.getReleaseName()), release);
                targetChainCacheManager.registerNewRelease(release);
            }

            Assert.notNull(release, "Release could not be determined.... How can there be a game version without a release.");

            releasesToUpdate.add(release);

            MappingCacheEntry cacheEntry = CacheUtils.getOutputMappingCacheEntryFromInput(
                    evm,
                    dependentMappingCacheManager,
                    targetChainCacheManager
            );

            final MappableDMO targetMappable;
            final UUID versionedMappableId;
            if (cacheEntry != null)
            {
                //Second mapping stage to something like a method or class.
                //Versioned mappable already exists.
                //Grab the versioned mappable from it.
                targetMappable = dependentMappingCacheManager.getMappable(cacheEntry.getMappableId());
                versionedMappableId = cacheEntry.getVersionedMappableId();
            }
            else
            {
                //This is something like a parameter. Something that was introduced in a later stage of mapping and not by the dependent mappable.
                final MappingCacheEntry alreadyExistingMappable = CacheUtils.getInputMappingCacheEntry(evm, targetChainCacheManager);
                if (alreadyExistingMappable != null)
                {
                    targetMappable = targetChainCacheManager.getMappable(alreadyExistingMappable.getMappableId());

                    //The mappable already exists. So we will keep its mappable, but create a new versioned mappable for.
                    if (alreadyExistingMappable.getGameVersionId() != gameVersion.getId())
                    {
                        //New game version is being imported.
                        //Create a new mappable for it.
                        final VersionedMappableDMO newVersionedMappable = createVersionedMappable(
                                evm,
                                gameVersion,
                                targetMappable,
                                targetChainCacheManager
                        );

                        versionedMappablesToSave.add(newVersionedMappable);
                        versionedMappables.put(newVersionedMappable.getId(), newVersionedMappable);
                        versionedMappableId = newVersionedMappable.getId();

                    }
                    else
                    {
                        //Second release for the same game version. Grab the versioned mappable id form it.
                        versionedMappableId = alreadyExistingMappable.getVersionedMappableId();
                    }

                }
                else
                {
                    //Looks like this mapping introduces a new mappable.
                    mappablesToSave.put(evm, new MappableDMO(
                                    UUID.randomUUID(),
                                    Constants.SYSTEM_ID,
                                    Timestamp.from(Instant.now()),
                                    MappableTypeDMO.valueOf(evm.getMappableType().name())
                            )
                    );

                    targetMappable = mappablesToSave.get(evm);

                    final VersionedMappableDMO newVersionedMappable = createVersionedMappable(
                            evm,
                            gameVersion,
                            targetMappable,
                            targetChainCacheManager
                            );

                    versionedMappablesToSave.add(newVersionedMappable);
                    versionedMappables.put(newVersionedMappable.getId(), newVersionedMappable);
                    versionedMappableId = newVersionedMappable.getId();
                }
            }

            final MappingCacheEntry currentEntry = CacheUtils.getOutputMappingCacheEntry(evm, targetChainCacheManager);
            final boolean isOldMapping = currentEntry != null && currentEntry.getInput().equals(evm.getInput()) && currentEntry.getOutput().equals(evm.getOutput()) && versionedMappableId.equals(currentEntry.getVersionedMappableId()) && currentEntry.getDocumentation().equals(evm.getDocumentation());
            final UUID mappingId = isOldMapping ? currentEntry.getMappingId() : UUID.randomUUID();

            final ReleaseComponentDMO releaseComponent = new ReleaseComponentDMO(
                    UUID.randomUUID(),
                    release.getId(),
                    mappingId
            );
            releaseComponentsToSave.put(evm, releaseComponent);

            if (evm.isLocked() && !targetChainCacheManager.isLocked(gameVersion.getId(), mappingType.getId(), versionedMappableId)) {
                protectedMappablesToSave.put(evm, new ProtectedMappableInformationDMO(
                      UUID.randomUUID(),
                      versionedMappableId,
                      mappingType.getId()
                  )
                );
            }

            if (!isOldMapping) {
                final MappingDMO mapping = new MappingDMO(
                                mappingId,
                                Constants.SYSTEM_ID,
                                Timestamp.from(Instant.now()),
                                versionedMappableId,
                                mappingType.getId(),
                                evm.getInput(),
                                evm.getOutput(),
                                evm.getDocumentation(),
                                evm.getExternalDistribution().getDmo()
                );
                mappingsToSave.put(evm, mapping);

                CacheUtils.registerNewEntry(
                                targetMappable,
                                versionedMappables.get(versionedMappableId),
                                mapping,
                                releaseComponent,
                                targetChainCacheManager
                );
            }
        });

        if (mappablesToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(MappableDMO.class)
                    .using(Flux.fromIterable(mappablesToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new " + mappings.get(0).getMappableType().name().toLowerCase() + " mappables from: " + mappablesToSave.size() + " local new instances.");
        }

        if (versionedMappablesToSave.size() > 0) {
            final Long rowsUpdated = databaseClient.insert()
                    .into(VersionedMappableDMO.class)
                    .using(Flux.fromIterable(versionedMappablesToSave))
                    .fetch()
                    .all()
                    .count()
                    .doOnError(error -> LOGGER.error("Failed to save: ", error))
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new " + mappings.get(0).getMappableType().name().toLowerCase() + " versioned mappables from: " + versionedMappablesToSave.size() + " local new instances");
        }

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

            LOGGER.warn(String.format("Created: %d new %s mappings from: %d local new instances of: %s mappings", rowsUpdated, mappings.get(0).getMappableType().name().toLowerCase(), mappingsToSave.size(), mappingName));
        }

        if (protectedMappablesToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                                       .into(ProtectedMappableInformationDMO.class)
                                       .using(Flux.fromIterable(protectedMappablesToSave.values()))
                                       .fetch()
                                       .all()
                                       .count()
                                       .block();

            LOGGER.warn(String.format("Created: %d new %s protected mappables from: %d local new instances of: %s mappings", rowsUpdated, mappings.get(0).getMappableType().name().toLowerCase(), mappingsToSave.size(), mappingName));
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

            LOGGER.warn(String.format("Created: %d new %s release component from: %d local new instances of: %s mappings", rowsUpdated, mappings.get(0).getMappableType().name().toLowerCase(), releaseComponentsToSave.size(), mappingName));
        }

        releasesToUpdate.forEach(release -> {
            release.setState(mappings.get(0).getMappableType().name().toLowerCase());

            databaseClient.update()
                    .table(ReleaseDMO.class)
                    .using(release)
                    .fetch()
                    .rowsUpdated()
                    .block();
        });
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

    private Map<UUID, VersionedMappableDMO> getVersionedMappablesForGameVersion(String version) {
        return databaseClient.execute(String.format("SELECT vc.* from versioned_mappable vc JOIN game_version gv on vc.game_version_id = gv.id WHERE gv.name = '%s'", version))
                .as(VersionedMappableDMO.class)
                .fetch()
                .all()
                .collectMap(VersionedMappableDMO::getId, Function.identity())
                .block();
    }

    private static VersionedMappableDMO createVersionedMappable(
            final ExternalMapping mapping,
            final GameVersionDMO gameVersion,
            final MappableDMO mappable,
            final AbstractMappingCacheManager targetCacheManager
    )
    {
        return new VersionedMappableDMO(
                UUID.randomUUID(),
                Constants.SYSTEM_ID,
                Timestamp.from(Instant.now()),
                gameVersion.getId(),
                mappable.getId(),
                VisibilityDMO.NOT_APPLICABLE,
                false,
                mapping.getType(),
                mapping.getParentClassMapping() == null ? null : targetCacheManager.getClassViaOutput(mapping.getParentClassMapping()).getVersionedMappableId(),
                mapping.getDescriptor(),
                mapping.getParentMethodMapping() == null ? null : targetCacheManager.getMethodViaOutput(mapping.getParentMethodMapping(), mapping.getParentClassMapping(), mapping.getParentMethodDescriptor()).getVersionedMappableId(),
                mapping.getSignature(),
                false,
                mapping.getIndex() == null ? -1 : mapping.getIndex()
        );
    }

    protected boolean isReleaseSnapshot(final String gameVersion, final String releaseName) {
        return GameVersionUtils.isPreRelease(gameVersion) || GameVersionUtils.isSnapshot(gameVersion);
    }

}
