package org.modmappings.crispycomputingmachine.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingBasedCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.*;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.*;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OfficialMappingWriter implements ItemWriter<ExternalVanillaMapping>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final R2dbcEntityTemplate                             databaseClient;
    private final VanillaAndExternalMappingBasedCacheManager mappingCacheManager;

    public OfficialMappingWriter(final R2dbcEntityTemplate databaseClient, final VanillaAndExternalMappingBasedCacheManager mappingCacheManager)
    {
        this.databaseClient = databaseClient;
        this.mappingCacheManager = mappingCacheManager;
    }

    @Override
    public void write(final List<? extends ExternalVanillaMapping> items)
    {
        final Map<String, GameVersionDMO> gameVersionsToSave = new LinkedHashMap<>();
        final Map<Tuple2<UUID, String>, ReleaseDMO> releasesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, MappableDMO> mappablesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, VersionedMappableDMO> versionedMappablesToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, MappingDMO> mappingsToSave = new LinkedHashMap<>();
        final Map<ExternalVanillaMapping, ReleaseComponentDMO> releaseComponentsToSave = new LinkedHashMap<>();
        final List<InheritanceDataDMO> inheritanceDataToSave = new LinkedList<>();
        final MappingTypeDMO officialMappingType = getOfficialMappingType();
        final MappingTypeDMO externalMappingType = getExternalMappingType();

        final Set<ReleaseDMO> releasesToUpdate = new HashSet<>();

        if (items.isEmpty())
        {
            return;
        }

        items.forEach(evm -> {
            if (OutputCacheUtils.getRemoteMappableId(mappingCacheManager, evm) == null)
            {
                mappablesToSave.put(evm, new MappableDMO(
                    UUID.randomUUID(),
                    Constants.SYSTEM_ID,
                    Timestamp.from(Instant.now()),
                    MappableTypeDMO.valueOf(evm.getMappableType().name())
                  )
                );
            }
            else
            {
                LOGGER.trace("Not recreating mappable for: " + evm);
            }

            final UUID mappableId = mappablesToSave.containsKey(evm) ? mappablesToSave.get(evm).getId() : OutputCacheUtils.getRemoteMappableId(mappingCacheManager, evm);

            GameVersionDMO gameVersion = gameVersionsToSave.containsKey(evm.getGameVersion()) ?
                                           gameVersionsToSave.get(evm.getGameVersion()) :
                                                                                          mappingCacheManager.getGameVersionFromName(evm.getGameVersion());

            ReleaseDMO officialMappingTypeRelease = releasesToSave.containsKey(Tuples.of(officialMappingType.getId(), evm.getGameVersion())) ?
                                                      releasesToSave.get(Tuples.of(officialMappingType.getId(), evm.getGameVersion())) :
                                                                                                                                         mappingCacheManager.getRelease(
                                                                                                                                           officialMappingType.getId(),
                                                                                                                                           evm.getGameVersion());
            ReleaseDMO externalMappingTypeRelease = releasesToSave.containsKey(Tuples.of(externalMappingType.getId(), evm.getGameVersion())) ?
                                                      releasesToSave.get(Tuples.of(externalMappingType.getId(), evm.getGameVersion())) :
                                                                                                                                         mappingCacheManager.getRelease(
                                                                                                                                           externalMappingType.getId(),
                                                                                                                                           evm.getGameVersion());

            if (gameVersion == null)
            {
                gameVersion = new GameVersionDMO(
                  UUID.randomUUID(),
                  Constants.SYSTEM_ID,
                  Timestamp.from(evm.getGameVersionReleaseDate().toInstant()),
                  evm.getGameVersion(),
                  GameVersionUtils.isPreRelease(evm.getGameVersion()),
                  GameVersionUtils.isSnapshot(evm.getGameVersion())
                );
                gameVersionsToSave.put(evm.getGameVersion(), gameVersion);

                officialMappingTypeRelease = new ReleaseDMO(
                  UUID.randomUUID(),
                  Constants.SYSTEM_ID,
                  Timestamp.from(evm.getGameVersionReleaseDate().toInstant()),
                  evm.getGameVersion(),
                  gameVersion.getId(),
                  officialMappingType.getId(),
                  GameVersionUtils.isPreRelease(evm.getGameVersion()) || GameVersionUtils.isSnapshot(evm.getGameVersion()),
                  MappableTypeDMO.PARAMETER.name()
                );
                externalMappingTypeRelease = new ReleaseDMO(
                  UUID.randomUUID(),
                  Constants.SYSTEM_ID,
                  Timestamp.from(evm.getGameVersionReleaseDate().toInstant()),
                  evm.getGameVersion(),
                  gameVersion.getId(),
                  externalMappingType.getId(),
                  GameVersionUtils.isPreRelease(evm.getGameVersion()) || GameVersionUtils.isSnapshot(evm.getGameVersion()),
                  MappableTypeDMO.PARAMETER.name()
                );
                releasesToSave.put(Tuples.of(officialMappingType.getId(), evm.getGameVersion()), officialMappingTypeRelease);
                releasesToSave.put(Tuples.of(externalMappingType.getId(), evm.getGameVersion()), externalMappingTypeRelease);

                mappingCacheManager.addGameVersion(gameVersion);
                mappingCacheManager.addRelease(officialMappingTypeRelease);
                mappingCacheManager.addRelease(externalMappingTypeRelease);
            }

            Assert.notNull(officialMappingTypeRelease, "Official Release could not be determined.... How can there be a game version without a officialMappingTypeRelease.");
            Assert.notNull(externalMappingTypeRelease, "External Release could not be determined.... How can there be a game version without a officialMappingTypeRelease.");

            releasesToUpdate.add(officialMappingTypeRelease);
            releasesToUpdate.add(externalMappingTypeRelease);

            final VersionedMappableDMO versionedMappable = createVersionedMappable(
              evm,
              gameVersion,
              mappableId,
              mappingCacheManager
            );
            versionedMappablesToSave.put(evm, versionedMappable);

            final MappingDMO mapping = new MappingDMO(
              UUID.randomUUID(),
              Constants.SYSTEM_ID,
              Timestamp.from(Instant.now()),
              versionedMappable.getId(),
              evm.isExternal() ? externalMappingType.getId() : officialMappingType.getId(),
              evm.getInput(),
              evm.getOutput(),
              "",
              evm.getExternalDistribution().getDmo(),
              null,
              null,
              gameVersion.getId(),
              MappableTypeDMO.valueOf(evm.getMappableType().name()),
              mappableId
            );
            mappingsToSave.put(evm, mapping);

            final ReleaseComponentDMO releaseComponent = new ReleaseComponentDMO(
              UUID.randomUUID(),
              evm.isExternal() ? externalMappingTypeRelease.getId() : officialMappingTypeRelease.getId(),
              mapping.getId()
            );
            releaseComponentsToSave.put(evm, releaseComponent);

            OutputCacheUtils.add(
              mappingCacheManager,
              versionedMappable,
              mapping
            );
        });

        items.stream().filter(evm -> evm.getMappableType() == ExternalMappableType.CLASS)
          .forEach(evm -> {
              final VersionedMappableDMO versionedMappable = versionedMappablesToSave.get(evm);

              evm.getSuperClasses().forEach(superMapping -> {
                  final UUID superId = mappingCacheManager.getVersionedMappableIdForClassFromOutput(superMapping);
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

              for (final MethodRef method : evm.getMethodOverrides())
              {
                  final UUID overridenMethod = mappingCacheManager.getVersionedMappableIdForMethod(method.getName(), method.getDesc(), method.getOwner());
                  if (overridenMethod == null)
                  {
                      LOGGER.warn("No override information found for: " + method);
                      continue;
                  }

                  final InheritanceDataDMO inheritanceData = new InheritanceDataDMO(
                    UUID.randomUUID(),
                    overridenMethod,
                    versionedMappable.getId()
                  );
                  inheritanceDataToSave.add(inheritanceData);
              }
          });

        items.stream().filter(evm -> evm.getMappableType() == ExternalMappableType.PARAMETER)
          .forEach(evm -> {
              final VersionedMappableDMO versionedMappable = versionedMappablesToSave.get(evm);

              for (final ParameterRef parameter : evm.getParameterOverrides())
              {
                  UUID overridenParameter = mappingCacheManager.getVersionedMappableIdForParameter(
                    parameter.getMethod() + "_" + parameter.getIndex(),
                    parameter.getOwner(),
                    parameter.getMethod(),
                    parameter.getDesc(),
                    parameter.getType());
                  if (overridenParameter == null)
                  {
                      //Okey this happens when the parameter is of a subtype of the requested type.
                      //We could ask the cache for all super types, but this is expensive.
                      //We know that we can trust MappingToy not to produce garbage descriptors and types
                      //We know the index, and the original descriptor since these are always correct
                      //So lets just rebuild the accurate type directly from the correct descriptor.
                      final AtomicInteger initialIndex = new AtomicInteger(evm.isStatic() ? 0 : 1);
                      final MethodDesc methodDesc = new MethodDesc(parameter.getDesc());
                      for (final String arg : methodDesc.getArgs())
                      {
                          if (initialIndex.get() == evm.getIndex())
                          {
                              overridenParameter = mappingCacheManager.getVersionedMappableIdForParameter(
                                parameter.getMethod() + "_" + parameter.getIndex(),
                                parameter.getOwner(),
                                parameter.getMethod(),
                                parameter.getDesc(),
                                arg);

                              if (overridenParameter != null)
                              {
                                  break;
                              }
                          }

                          if (arg.equals("D") || arg.equals("J"))
                          {
                              initialIndex.incrementAndGet();
                          }
                          initialIndex.incrementAndGet();
                      }

                      if (overridenParameter == null)
                      {
                          LOGGER.warn("No override information found for: " + parameter);
                          continue;
                      }
                  }

                  final InheritanceDataDMO inheritanceData = new InheritanceDataDMO(
                    UUID.randomUUID(),
                    overridenParameter,
                    versionedMappable.getId()
                  );
                  inheritanceDataToSave.add(inheritanceData);
              }
          });

        LOGGER.warn("Starting insertion of objects.");

        final List<VersionedMappableDMO> orderedVersionedMappablesToSave =
          versionedMappablesToSave.entrySet()
            .stream()
            .sorted(Comparator.comparing((Function<Map.Entry<ExternalVanillaMapping, VersionedMappableDMO>, Integer>) externalVanillaMappingVersionedMappableDMOEntry -> externalVanillaMappingVersionedMappableDMOEntry.getKey().getMappableType().ordinal())
                .thenComparing(Comparator.comparing((Function<Map.Entry<ExternalVanillaMapping, VersionedMappableDMO>, Long>) externalVanillaMappingVersionedMappableDMOEntry -> externalVanillaMappingVersionedMappableDMOEntry
                                                                                                                                                                                   .getKey()
                                                                                                                                                                                   .getOutput()
                                                                                                                                                                                   .chars()
                                                                                                                                                                                   .filter(
                                                                                                                                                                                     (c) ->
                                                                                                                                                                                       c
                                                                                                                                                                                         == '$')
                                                                                                                                                                                   .count())
                                 .thenComparing(externalVanillaMappingVersionedMappableDMOEntry -> externalVanillaMappingVersionedMappableDMOEntry.getKey().getOutput())))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

        final String gameVersionInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          GameVersionDMO.class,
          gameVersionsToSave.values()
        );
        final String releaseInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          ReleaseDMO.class,
          releasesToSave.values()
        );
        final String mappablesInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          MappableDMO.class,
          mappablesToSave.values()
        );
        final String versionedMappablesInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          VersionedMappableDMO.class,
          orderedVersionedMappablesToSave
        );
        final String mappingsInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          MappingDMO.class,
          mappingsToSave.values()
        );
        final String releaseComponentsInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          ReleaseComponentDMO.class,
          releaseComponentsToSave.values()
        );
        final String inheritanceDataInsertionStatement = DatabaseUtils.createInsertForAll(
          databaseClient,
          InheritanceDataDMO.class,
          inheritanceDataToSave
        );

        final String statement = gameVersionInsertionStatement + "\n"
                                   + releaseInsertionStatement + "\n"
                                   + mappablesInsertionStatement + "\n"
                                   + versionedMappablesInsertionStatement + "\n"
                                   + mappingsInsertionStatement + "\n"
                                   + releaseComponentsInsertionStatement + "\n"
                                   + inheritanceDataInsertionStatement;

        final String transactionStatement = "BEGIN;\n"
                                              + statement + "\n"
                                              + "COMMIT;";

        DatabaseUtils.createPrebuildSimpleStatement(
          databaseClient,
          transactionStatement
        )
          .flatMap(s -> Mono.from(s.execute()))
          .flatMap(r -> Mono.from(r.getRowsUpdated()))
          .block();

        LOGGER.warn("Finished inserting of objects. Total rows inserted:");
        LOGGER.warn("  > GameVersions: " + gameVersionsToSave.size());
        LOGGER.warn("  > Releases: " + releasesToSave.size());
        LOGGER.warn("  > Mappables: " + mappablesToSave.size());
        LOGGER.warn("  > VersionedMappables: " + orderedVersionedMappablesToSave.size());
        LOGGER.warn("  > Mappings: " + mappingsToSave.size());
        LOGGER.warn("  > ReleaseComponents: " + releaseComponentsToSave.size());
        LOGGER.warn("  > InheritanceData: " + inheritanceDataToSave.size());

        LOGGER.warn("Running post processing steps.");
        LOGGER.info("Updating packages.");

        DatabaseUtils.updatePackages(databaseClient);

        LOGGER.info("Updated packages.");
        LOGGER.warn("Ran post processing steps.");
    }

    private MappingTypeDMO getOfficialMappingType()
    {
        return databaseClient.select(MappingTypeDMO.class)
                 .matching(Query.query(Criteria.where("name").is(Constants.OFFICIAL_MAPPING_NAME)))
                 .first()
                 .switchIfEmpty(Mono.just(new MappingTypeDMO(UUID.randomUUID(),
                   Constants.SYSTEM_ID,
                   Timestamp.from(Instant.now()),
                   Constants.OFFICIAL_MAPPING_NAME,
                   false,
                   false,
                   Constants.OFFICIAL_MAPPING_STATE_IN,
                   Constants.OFFICIAL_MAPPING_STATE_OUT))
                                  .flatMap(mappingType -> databaseClient.insert(MappingTypeDMO.class)
                                                            .using(mappingType)
                                                            .map(r -> mappingType))
                 ).block();
    }

    private MappingTypeDMO getExternalMappingType()
    {
        return databaseClient.select(MappingTypeDMO.class)
                 .matching(Query.query(Criteria.where("name").is(Constants.EXTERNAL_MAPPING_NAME)))
                 .first()
                 .switchIfEmpty(Mono.just(new MappingTypeDMO(UUID.randomUUID(),
                   Constants.SYSTEM_ID,
                   Timestamp.from(Instant.now()),
                   Constants.EXTERNAL_MAPPING_NAME,
                   false,
                   false,
                   Constants.EXTERNAL_MAPPING_STATE_IN,
                   Constants.EXTERNAL_MAPPING_STATE_OUT))
                                  .flatMap(mappingType -> databaseClient.insert(MappingTypeDMO.class)
                                                            .using(mappingType)
                                                            .map(r -> mappingType))
                 ).block();
    }

    private static VersionedMappableDMO createVersionedMappable(
      final ExternalVanillaMapping externalVanillaMapping,
      final GameVersionDMO gameVersion,
      final UUID mappable,
      final VanillaAndExternalMappingBasedCacheManager mappingCacheManager
    )
    {
        return new VersionedMappableDMO(
          UUID.randomUUID(),
          Constants.SYSTEM_ID,
          Timestamp.from(Instant.now()),
          gameVersion.getId(),
          mappable,
          MappableTypeDMO.valueOf(externalVanillaMapping.getMappableType().name()),
          externalVanillaMapping.getParentClassMapping() == null
            ? null
            : mappingCacheManager.getVersionedMappableIdForClassFromOutput(externalVanillaMapping.getParentClassMapping()),
          externalVanillaMapping.getParentMethodMapping() == null
            ? null
            : mappingCacheManager.getVersionedMappableIdForMethod(externalVanillaMapping.getParentMethodMapping(),
              externalVanillaMapping.getParentMethodDescriptor(),
              externalVanillaMapping.getParentClassMapping()),
          VisibilityDMO.valueOf(externalVanillaMapping.getVisibility().name()),
          externalVanillaMapping.isStatic(),
          externalVanillaMapping.getType(),
          externalVanillaMapping.getDescriptor(),
          externalVanillaMapping.getSignature(),
          externalVanillaMapping.isExternal(),
          externalVanillaMapping.getIndex() == null ? -1 : externalVanillaMapping.getIndex()
        );
    }
}
