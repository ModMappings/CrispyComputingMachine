package org.modmappings.crispycomputingmachine.writers.chain.initial;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.GameVersionUtils;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VisibilityDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.DistributionDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.batch.core.configuration.xml.StandaloneStepParser;
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
import java.util.function.IntBinaryOperator;

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
        final Map<ExternalMapping, MappableDMO> mappablesToSave = new LinkedHashMap<>();
        final Map<Tuple2<UUID, String>, ReleaseDMO> releasesToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, MappingDMO> mappingsToSave = new LinkedHashMap<>();
        final Map<ExternalMapping, ReleaseComponentDMO> releaseComponentsToSave = new LinkedHashMap<>();

        final Set<ReleaseDMO> releasesToUpdate = new HashSet<>();
        final Set<VersionedMappableDMO> versionedMappablesToUpdate = new HashSet<>();

        if (items.isEmpty())
            return;

        final ExternalMapping first = items.get(0);
        final Map<UUID, VersionedMappableDMO> versionedMappables = getVersionedMappablesForGameVersion(first.getGameVersion());

        items.forEach(evm -> {
            if (evm.getMappableType() == ExternalMappableType.PARAMETER && evm.getParentMethodMapping().equals("func_220073_a"))
                System.out.println("Found it");

            if (!CacheUtils.alreadyExistsOnInput(evm, vanillaAndExternalMappingCacheManager, targetChainCacheManager))
            {
                LOGGER.warn("Could not find a vanilla mapping for: " + evm);
                return;
            }

            GameVersionDMO gameVersion = targetChainCacheManager.getGameVersion(evm.getGameVersion());

            if(gameVersion == null)
            {
                LOGGER.error("Could not find game version with name: " + evm.getGameVersion() + "! This is not supposed to be possible. And things might break!");
                return;
            }

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
                        GameVersionUtils.isPreRelease(evm.getGameVersion()) || GameVersionUtils.isSnapshot(evm.getGameVersion()),
                        "new"
                );

                releasesToSave.put(Tuples.of(mappingType.getId(), evm.getReleaseName()), release);
                targetChainCacheManager.registerNewRelease(release);
            }

            final MappableDMO vanillaMappable = CacheUtils.getCachedMappableViaInput(evm, vanillaAndExternalMappingCacheManager, targetChainCacheManager);
            MappableDMO targetMappable = CacheUtils.getCachedMappableViaOutput(evm, targetChainCacheManager);
            if (vanillaMappable != null && vanillaMappable != targetMappable && shouldCorrectVanilla())
            {
                //Information is conflicting.
                //We need to check several cases and updated mappables and versioned mappables accordingly.
                final List<VersionedMappableDMO> alreadyKnownVersionedMappables = getVersionedMappablesForMappable(vanillaMappable);
                if (alreadyKnownVersionedMappables.size() == 1 && alreadyKnownVersionedMappables.get(0).getGameVersionId().equals(gameVersion.getId()))
                {
                    LOGGER.debug("Vanilla mappable: " + vanillaMappable + " is also new. Not correcting.");
                    targetMappable = vanillaMappable;
                }
                else
                {
                    LOGGER.warn("A discrepancy between: " + mappingName + " and vanilla was found. " + mappingName + " declares " + evm + " to belong to a different mapping not related. Attempting to correct vanilla.");
                    if (targetMappable == null) {
                        LOGGER.warn("Creating a new mappable for: " + evm + " since " + mappingName + " declared it to be a new mapping. Not related.");
                        mappablesToSave.put(evm, new MappableDMO(
                                        UUID.randomUUID(),
                                        Constants.SYSTEM_ID,
                                        Timestamp.from(Instant.now()),
                                        MappableTypeDMO.valueOf(evm.getMappableType().name())
                                )
                        );

                        targetMappable = mappablesToSave.get(evm);
                    }

                    final UUID mappableId = targetMappable.getId();
                    //This should exist, else what the hell is vanilla telling us.
                    final VersionedMappableDMO toUpdate = alreadyKnownVersionedMappables.stream().filter(vm -> vm.getGameVersionId() == gameVersion.getId()).findFirst().map(toEdit -> new VersionedMappableDMO(
                            toEdit.getId(),
                            toEdit.getCreatedBy(),
                            toEdit.getCreatedOn(),
                            toEdit.getGameVersionId(),
                            mappableId,
                            toEdit.getVisibility(),
                            toEdit.isStatic(),
                            toEdit.getType(),
                            toEdit.getParentClassId(),
                            toEdit.getDescriptor(),
                            toEdit.getParentMethodId(),
                            toEdit.getSignature(),
                            toEdit.isExternal(),
                            toEdit.getIndex()
                    )).orElseThrow();

                    //Store and update maps.
                    versionedMappablesToUpdate.add(toUpdate);
                    versionedMappables.put(toUpdate.getId(), toUpdate);
                    alreadyKnownVersionedMappables.removeIf(vm -> vm.getGameVersionId() == gameVersion.getId());
                }
            }
            else if (vanillaMappable != null && vanillaMappable != targetMappable)
            {
                LOGGER.warn(String.format("Found a discrepancy between: %s and vanilla. The mapping: %s does not have the same mappables: %s and %s Since this writer is now allowed to correct. Vanilla will be leading.", mappingName, evm, vanillaMappable, targetMappable));
                targetMappable = vanillaMappable;
            }

            Assert.notNull(release, "Release could not be determined.... How can there be a game version without a release.");

            releasesToUpdate.add(release);

            final UUID vanillaVersionedMappableId = CacheUtils.getInputMappingCacheEntry(
                    evm,
                    vanillaAndExternalMappingCacheManager,
                    targetChainCacheManager
            ).getVersionedMappableId();

            final MappingCacheEntry currentEntry = CacheUtils.getOutputMappingCacheEntry(evm, targetChainCacheManager);
            final boolean isOldMapping = currentEntry != null && currentEntry.getInput().equals(evm.getInput()) && currentEntry.getOutput().equals(evm.getOutput()) && vanillaVersionedMappableId.equals(currentEntry.getVersionedMappableId());
            final UUID mappingId = isOldMapping ? currentEntry.getMappingId() : UUID.randomUUID();

            final ReleaseComponentDMO releaseComponent = new ReleaseComponentDMO(
                    UUID.randomUUID(),
                    release.getId(),
                    mappingId
            );
            releaseComponentsToSave.put(evm, releaseComponent);

            if (!isOldMapping)
            {
                final MappingDMO mapping = new MappingDMO(
                                mappingId,
                                Constants.SYSTEM_ID,
                                Timestamp.from(Instant.now()),
                                vanillaVersionedMappableId,
                                mappingType.getId(),
                                evm.getInput(),
                                evm.getOutput(),
                                "",
                                evm.getExternalDistribution().getDmo()
                );
                mappingsToSave.put(evm, mapping);

                CacheUtils.registerNewEntry(
                                targetMappable,
                                versionedMappables.get(vanillaVersionedMappableId),
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

            LOGGER.warn("Created: " + rowsUpdated + " new " + items.get(0).getMappableType().name().toLowerCase() + " mappables from: " + mappablesToSave.size() + " local new instances.");
        }

        if (versionedMappablesToUpdate.size() > 0) {
            final Long rowsUpdated = (long) Flux.fromIterable(versionedMappablesToUpdate).flatMap(vm -> databaseClient.update()
                    .table(VersionedMappableDMO.class)
                    .using(vm)
                    .fetch()
                    .rowsUpdated())
                    .collectList()
                    .map(l -> l.stream().mapToInt(i -> i).reduce(new IntBinaryOperator() {
                        @Override
                        public int applyAsInt(final int i, final int i1) {
                            return i + i1;
                        }
                    }))
                    .doOnError(error -> LOGGER.error("Failed to save: ", error))
                    .block()
                    .getAsInt();

            LOGGER.warn("Updated: " + rowsUpdated + " " + items.get(0).getMappableType().name().toLowerCase() + " versioned mappables from: " + versionedMappablesToUpdate.size() + " local updated instances");
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

    private List<VersionedMappableDMO> getVersionedMappablesForMappable(MappableDMO mappableDMO) {
        return databaseClient.execute(String.format("SELECT vc.* from versioned_mappable vc WHERE vc.mappable_id = '%s'", mappableDMO.getId()))
                .as(VersionedMappableDMO.class)
                .fetch()
                .all()
                .collectList()
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
