package org.modmappings.crispycomputingmachine.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.MappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodRef;
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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalVanillaMappingWriter implements ItemWriter<ExternalVanillaMapping> {

    private static final Logger LOGGER = LogManager.getLogger(ExternalVanillaMappingWriter.class);

    private final DatabaseClient databaseClient;
    private final MappingCacheManager mappingCacheManager;

    public ExternalVanillaMappingWriter(final DatabaseClient databaseClient, final MappingCacheManager mappingCacheManager) {
        this.databaseClient = databaseClient;
        this.mappingCacheManager = mappingCacheManager;
    }

    @Override
    public void write(final List<? extends ExternalVanillaMapping> items) throws Exception {
        final Map<String, GameVersionDMO> gameVersionsToSave = new LinkedHashMap<>();
        final Map<String, ReleaseDMO> releasesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, MappableDMO> mappablesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, VersionedMappableDMO> versionedMappablesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, MappingDMO> mappingsToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, ReleaseComponentDMO> releaseComponentsToSave = new LinkedHashMap<>();
        final List<InheritanceDataDMO> inheritanceDataToSave = new LinkedList<>();
        final MappingTypeDMO officialMappingType = getOfficialMappingType();

        items.forEach(evm -> {
                    if (evm.getParentClassMapping() != null && evm.getParentClassMapping().equals("cwq$a"))
                        System.out.println("Found it");

                    if (!CacheUtils.vanillaAlreadyExists(evm, mappingCacheManager))
                    {
                        mappablesToSave.put(evm, new MappableDMO(
                                UUID.randomUUID(),
                                Constants.SYSTEM_ID,
                                Timestamp.from(Instant.now()),
                                MappableTypeDMO.valueOf(evm.getMappableType().name())
                            )
                        );
                    }

                    final MappableDMO mappable = mappablesToSave.containsKey(evm) ? mappablesToSave.get(evm) : CacheUtils.getCachedMappable(evm, mappingCacheManager);

                    GameVersionDMO gameVersion = gameVersionsToSave.containsKey(evm.getGameVersion()) ?
                            gameVersionsToSave.get(evm.getGameVersion()) :
                            mappingCacheManager.getGameVersion(evm.getGameVersion());

                    ReleaseDMO release = releasesToSave.containsKey(evm.getGameVersion()) ?
                            releasesToSave.get(evm.getGameVersion()) :
                            mappingCacheManager.getRelease(evm.getGameVersion());

                    if (gameVersion == null)
                    {
                        gameVersion = new GameVersionDMO(
                            UUID.randomUUID(),
                            Constants.SYSTEM_ID,
                            Timestamp.from(evm.getGameVersionReleaseDate().toInstant()),
                            evm.getGameVersion(),
                            isPreRelease(evm.getGameVersion()),
                            isSnapshot(evm.getGameVersion())
                        );
                        gameVersionsToSave.put(evm.getGameVersion(), gameVersion);

                        release = new ReleaseDMO(
                                UUID.randomUUID(),
                                Constants.SYSTEM_ID,
                                Timestamp.from(evm.getGameVersionReleaseDate().toInstant()),
                                evm.getGameVersion(),
                                gameVersion.getId(),
                                officialMappingType.getId(),
                                isPreRelease(evm.getGameVersion()) || isSnapshot(evm.getGameVersion())
                        );
                        releasesToSave.put(evm.getGameVersion(), release);

                        mappingCacheManager.registerNewGameVersion(gameVersion);
                        mappingCacheManager.registerNewRelease(release);
                    }

                    Assert.notNull(release, "Release could not be determined.... How can there be a game version without a release.");

                    final VersionedMappableDMO versionedMappable = createVersionedMappable(
                            evm,
                            gameVersion,
                            mappable,
                            mappingCacheManager
                    );
                    versionedMappablesToSave.put(evm, versionedMappable);

                    final MappingDMO mapping = new MappingDMO(
                            UUID.randomUUID(),
                            Constants.SYSTEM_ID,
                            Timestamp.from(Instant.now()),
                            versionedMappable.getId(),
                            officialMappingType.getId(),
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
                            versionedMappable,
                            mapping,
                            mappingCacheManager
                    );
                });

        items.stream().filter(evm -> evm.getMappableType() == ExternalMappableType.CLASS)
                .forEach(evm -> {
                    final VersionedMappableDMO versionedMappable = versionedMappablesToSave.get(evm);

                    evm.getSuperClasses().forEach(superMapping -> {
                        final UUID superId = mappingCacheManager.getClass(superMapping).getVersionedMappableId();
                        final InheritanceDataDMO inheritanceData = new InheritanceDataDMO(
                                UUID.randomUUID(),
                                superId,
                                versionedMappable.getId()
                        );
                        inheritanceDataToSave.add(inheritanceData);
                    });
                });


        items.stream().filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
                .forEach(evm -> {
                    final VersionedMappableDMO versionedMappable = versionedMappablesToSave.get(evm);

                    for (final MethodRef method : evm.getMethodOverrides()) {
                        final MappingCacheEntry overridenMethod = mappingCacheManager.getMethod(method.getName(), method.getOwner(), method.getDesc());
                        if (overridenMethod == null)
                            continue;

                        final UUID overridenId = overridenMethod.getVersionedMappableId();

                        final InheritanceDataDMO inheritanceData = new InheritanceDataDMO(
                                UUID.randomUUID(),
                                overridenId,
                                versionedMappable.getId()
                        );
                        inheritanceDataToSave.add(inheritanceData);
                    }
                });

        if (gameVersionsToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(GameVersionDMO.class)
                    .using(Flux.fromIterable(gameVersionsToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new game versions from: " + gameVersionsToSave.size() + " local new instances");
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

            LOGGER.warn("Created: " + rowsUpdated + " new releases from: " + gameVersionsToSave.size() + " local new instances");
        }

        if (mappablesToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(MappableDMO.class)
                    .using(Flux.fromIterable(mappablesToSave.values()))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new mappables from: " + mappablesToSave.size() + " local new instances.");
        }

        if (versionedMappablesToSave.size() > 0)
        {
            final List<VersionedMappableDMO> savingOrder =
                    versionedMappablesToSave.entrySet().stream().sorted(Comparator.comparing((Function<Map.Entry<ExternalVanillaMapping, VersionedMappableDMO>, Long>) externalVanillaMappingVersionedMappableDMOEntry -> externalVanillaMappingVersionedMappableDMOEntry.getKey().getOutput().chars().filter((c) -> c == '$').count()).thenComparing(externalVanillaMappingVersionedMappableDMOEntry -> externalVanillaMappingVersionedMappableDMOEntry.getKey().getOutput())).map(Map.Entry::getValue).collect(Collectors.toList());

            final Long rowsUpdated = databaseClient.insert()
                    .into(VersionedMappableDMO.class)
                    .using(Flux.fromIterable(savingOrder))
                    .fetch()
                    .all()
                    .count()
                    .doOnError(error -> LOGGER.error("Failed to save: ", error))
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new versioned mappables from: " + versionedMappablesToSave.size() + " local new instances");
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

            LOGGER.warn("Created: " + rowsUpdated + " new mappings from: " + mappingsToSave.size() + " local new instances");
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

            LOGGER.warn("Created: " + rowsUpdated + " new release component from: " + releaseComponentsToSave.size() + " local new instances");
        }

        if (inheritanceDataToSave.size() > 0)
        {
            final Long rowsUpdated = databaseClient.insert()
                    .into(InheritanceDataDMO.class)
                    .using(Flux.fromIterable(inheritanceDataToSave))
                    .fetch()
                    .all()
                    .count()
                    .block();

            LOGGER.warn("Created: " + rowsUpdated + " new inheritance data entries from: " + inheritanceDataToSave.size() + " local new instances");
        }


    }

    private MappingTypeDMO getOfficialMappingType() {
        return databaseClient.select()
                .from(MappingTypeDMO.class)
                .matching(Criteria.where("name").is(Constants.OFFICIAL_MAPPING_NAME))
                .fetch()
                .first()
                .switchIfEmpty(Mono.just(new MappingTypeDMO(UUID.randomUUID(), Constants.SYSTEM_ID, Timestamp.from(Instant.now()), Constants.OFFICIAL_MAPPING_NAME, false, false, Constants.OFFICIAL_MAPPING_STATE_IN, Constants.OFFICIAL_MAPPING_STATE_OUT))
                        .flatMap(mappingType -> databaseClient.insert()
                            .into(MappingTypeDMO.class)
                            .using(mappingType)
                            .fetch()
                            .first()
                            .map(r -> mappingType))
                ).block();
    }

    public MappingCacheManager getMappingCacheManager() {
        return mappingCacheManager;
    }

    private static VersionedMappableDMO createVersionedMappable(
            final ExternalVanillaMapping externalVanillaMapping,
            final GameVersionDMO gameVersion,
            final MappableDMO mappable,
            final MappingCacheManager mappingCacheManager
    )
    {
        return new VersionedMappableDMO(
                UUID.randomUUID(),
                Constants.SYSTEM_ID,
                Timestamp.from(Instant.now()),
                gameVersion.getId(),
                mappable.getId(),
                VisibilityDMO.valueOf(externalVanillaMapping.getVisibility().name()),
                externalVanillaMapping.isStatic(),
                externalVanillaMapping.getType(),
                externalVanillaMapping.getParentClassMapping() == null ? null : mappingCacheManager.getClass(externalVanillaMapping.getParentClassMapping()).getVersionedMappableId(),
                externalVanillaMapping.getDescriptor(),
                externalVanillaMapping.getParentMethodMapping() == null ? null : mappingCacheManager.getMethod(externalVanillaMapping.getParentMethodMapping(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getParentMethodDescriptor()).getVersionedMappableId(),
                externalVanillaMapping.getSignature()
        );
    }

    private static boolean isPreRelease(String version)
    {
        String lower = version.toLowerCase(Locale.ENGLISH);

        if ("15w14a".equals(lower)) { //2015 April Fools
            return false;
        } else if ("1.rv-pre1".equals(lower)) { //2016 April Fools
            return false;
        } else if ("3d shareware v1.34".equals(lower)) { //2019 April Fools
            return false;
        } else if (lower.charAt(0) == 'b' || lower.charAt(0) == 'a') {
            return false;
        } else if (lower.length() == 6 && lower.charAt(2) == 'w') {
            return false;
        } else {
            if (lower.contains("-pre")) {
                return true;
            } else if (lower.contains("_Pre-Release_".toLowerCase())) {
                return true;
            } else return lower.contains(" Pre-Release ".toLowerCase());
        }
    }

    private static boolean isSnapshot(String version)
    {
        String lower = version.toLowerCase(Locale.ENGLISH);
        switch (lower) {
            case "15w14a":  //2015 April Fools
                return true;
            case "1.rv-pre1":  //2016 April Fools
                return true;
            case "3d shareware v1.34":  //2019 April Fools
                return true;
            default:
                return lower.length() == 6 && lower.charAt(2) == 'w';
        }
    }
}
