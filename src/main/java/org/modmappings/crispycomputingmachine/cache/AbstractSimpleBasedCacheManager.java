package org.modmappings.crispycomputingmachine.cache;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodDesc;
import org.modmappings.crispycomputingmachine.utils.RemappableType;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractSimpleBasedCacheManager
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String INPUT_CLASS_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.input as name, m.mappable_id\n"
                                                                              + "    from mapping m\n"
                                                                              + "    where m.mapping_type_id in (%s)\n"
                                                                              + "        and m.mappable_type = 'CLASS'\n"
                                                                              + "    group by m.input, m.mappable_id;";

    public static final String INPUT_CLASS_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.input as name, m.versioned_mappable_id from\n"
                                                                                        + "    (SELECT m.input, m.mappable_id, max(m.created_on) as created_on\n"
                                                                                        + "     from mapping m\n"
                                                                                        + "     where m.mapping_type_id in (%s)\n"
                                                                                        + "        and m.mappable_type = 'CLASS'\n"
                                                                                        + "    group by m.input, m.mappable_id) as latest_mappings\n"
                                                                                        + "    join mapping m on m.input = latest_mappings.input and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on;";
    
    public static final String OUTPUT_CLASS_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, m.mappable_id\n"
                                                                       + "    from mapping m\n"
                                                                       + "    where m.mapping_type_id in (%s)\n"
                                                                       + "        and m.mappable_type = 'CLASS'\n"
                                                                       + "    group by m.output, m.mappable_id;";

    public static final String OUTPUT_CLASS_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, m.versioned_mappable_id from\n"
                                    + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                    + "     from mapping m\n"
                                    + "     where m.mapping_type_id in (%s)\n"
                                    + "        and m.mappable_type = 'CLASS'\n"
                                    + "    group by m.output, m.mappable_id) as latest_mappings\n"
                                    + "    join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on;";

    public static final String METHOD_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.descriptor, m.mappable_id, vm.parent_class_id as parent_class_identifier from\n"
                                                                        + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                        + "     from mapping m\n"
                                                                        + "     where m.mapping_type_id in (%s)\n"
                                                                        + "       and m.mappable_type = 'METHOD'\n"
                                                                        + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                        + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                        + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String METHOD_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.descriptor, m.versioned_mappable_id, vm.parent_class_id as parent_class_identifier from\n"
                                                                                  + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                                  + "     from mapping m\n"
                                                                                  + "     where m.mapping_type_id in (%s)\n"
                                                                                  + "       and m.mappable_type = 'METHOD'\n"
                                                                                  + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                                  + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                                  + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String FIELD_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.type, m.mappable_id, vm.parent_class_id as parent_class_identifier from\n"
                                                                       + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                       + "     from mapping m\n"
                                                                       + "     where m.mapping_type_id in (%s)\n"
                                                                       + "       and m.mappable_type = 'FIELD'\n"
                                                                       + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                       + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                       + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String FIELD_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.type, m.versioned_mappable_id, vm.parent_class_id as parent_class_identifier from\n"
                                                                                 + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                                 + "     from mapping m\n"
                                                                                 + "     where m.mapping_type_id in (%s)\n"
                                                                                 + "       and m.mappable_type = 'FIELD'\n"
                                                                                 + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                                 + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                                 + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String PARAMETER_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.type, m.mappable_id, vm.parent_class_id as parent_class_identifier, vm.parent_method_id as parent_method_identifier from\n"
                                                                           + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                           + "     from mapping m\n"
                                                                           + "     where m.mapping_type_id in (%s)\n"
                                                                           + "       and m.mappable_type = 'PARAMETER'\n"
                                                                           + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                           + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                           + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String PARAMETER_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT = "SELECT m.output as name, vm.type, m.versioned_mappable_id, vm.parent_class_id as parent_class_identifier, vm.parent_method_id as parent_method_identifier from\n"
                                                                              + "    (SELECT m.output, m.mappable_id, max(m.created_on) as created_on\n"
                                                                              + "     from mapping m\n"
                                                                              + "     where m.mapping_type_id in (%s)\n"
                                                                              + "       and m.mappable_type = 'PARAMETER'\n"
                                                                              + "     group by m.output, m.mappable_id) as latest_mappings\n"
                                                                              + "        join mapping m on m.output = latest_mappings.output and m.mappable_id = latest_mappings.mappable_id and m.created_on = latest_mappings.created_on\n"
                                                                              + "        join versioned_mappable vm on m.versioned_mappable_id = vm.id and m.game_version_id = vm.game_version_id and m.mappable_id = vm.mappable_id and m.mappable_type = vm.mappable_type;";

    public static final String GAMEVERSION_LOAD_QUERY_FORMAT = "SELECT gv.* from game_version gv;";

    public static final String RELEASE_LOAD_QUERY_FORMAT = "SELECT r.* from release r\n"
                                                             + "    where r.mapping_type_id in (%s);";

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final CacheEntry<Map<ClassIdentifier, UUID>> inputClassMIdCache  = new CacheEntry<>(Maps::newHashMap);
    private final CacheEntry<Map<ClassIdentifier, UUID>> inputClassVMIdCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<ClassIdentifier, UUID>> outputClassMIdCache  = new CacheEntry<>(Maps::newHashMap);
    private final CacheEntry<Map<ClassIdentifier, UUID>> outputClassVMIdCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<MethodIdentifier, UUID>> methodMIdCache = new CacheEntry<>(Maps::newHashMap);
    private final CacheEntry<Map<MethodIdentifier, UUID>> methodVMIdCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<FieldIdentifier, UUID>> fieldMIdCache = new CacheEntry<>(Maps::newHashMap);
    private final CacheEntry<Map<FieldIdentifier, UUID>> fieldVMIdCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<ParameterIdentifier, UUID>> parameterMIdCache = new CacheEntry<>(Maps::newHashMap);
    private final CacheEntry<Map<ParameterIdentifier, UUID>> parameterVMIdCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<String, GameVersionDMO>> namedGameVersionCache = new CacheEntry<>(Maps::newHashMap);

    private final CacheEntry<Map<ReleaseIdentifier, ReleaseDMO>> releaseCache = new CacheEntry<>(Maps::newHashMap);

    protected AbstractSimpleBasedCacheManager(final R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    protected UUID getOrCreateIdForMappingType(
      final String mappingTypeName,
      final boolean isVisible,
      final boolean isEditable,
      final String stateIn,
      final String stateOut
    )
    {
        return r2dbcEntityTemplate.select(MappingTypeDMO.class)
                 .matching(Query.query(Criteria.where("name").is(mappingTypeName)))
                 .first()
                 .switchIfEmpty(
                   Mono.just(new MappingTypeDMO(UUID.randomUUID(),
                     Constants.SYSTEM_ID,
                     Timestamp.from(Instant.now()),
                     mappingTypeName,
                     isVisible,
                     isEditable,
                     stateIn,
                     stateOut))
                     .flatMap(mappingType -> r2dbcEntityTemplate.insert(MappingTypeDMO.class)
                                               .using(mappingType)
                                               .map(r -> mappingType))
                 )
                 .map(MappingTypeDMO::getId).blockOptional().orElse(null);
    }

    protected abstract List<UUID> getMappingTypeIds();

    private String buildMappingIdFilter()
    {
        return getMappingTypeIds().stream()
                 .map(UUID::toString)
                 .map(id -> String.format("'%s'", id))
                 .collect(Collectors.joining(", "));
    }

    public UUID getMappableIdForClassFromInput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadMappableClassCacheForOutput(inputClassMIdCache, "MappableId from Class by Input", INPUT_CLASS_MAPPABLE_ID_LOAD_QUERY_FORMAT);

        return inputClassMIdCache.getLocalData().getOrDefault(
          classIdentifier,
          inputClassMIdCache.getRemoteData().getOrDefault(
            classIdentifier,
            null
          )
        );
    }

    public UUID getVersionedMappableIdForClassFromInput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadVersionedMappableClassCacheForOutput(inputClassVMIdCache,
          "VersionedMappableId from Class by Input",
          INPUT_CLASS_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT
        );

        return inputClassVMIdCache.getLocalData().getOrDefault(
          classIdentifier,
          inputClassVMIdCache.getRemoteData().getOrDefault(
            classIdentifier,
            null
          )
        );
    }
    
    public UUID getMappableIdForClassFromOutput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadMappableClassCacheForOutput(outputClassMIdCache, "MappableId from Class by Output", OUTPUT_CLASS_MAPPABLE_ID_LOAD_QUERY_FORMAT);

        return outputClassMIdCache.getLocalData().getOrDefault(
          classIdentifier,
          outputClassMIdCache.getRemoteData().getOrDefault(
            classIdentifier,
            null
          )
        );
    }

    public UUID getRemoteMappableIdForClassFromOutput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadMappableClassCacheForOutput(outputClassMIdCache, "MappableId from Class by Output", OUTPUT_CLASS_MAPPABLE_ID_LOAD_QUERY_FORMAT);

        return outputClassMIdCache.getRemoteData().getOrDefault(
          classIdentifier,
          null
        );
    }

    public UUID getVersionedMappableIdForClassFromOutput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadVersionedMappableClassCacheForOutput(outputClassVMIdCache,
          "VersionedMappableId from Class by Output",
          OUTPUT_CLASS_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT
        );

        return outputClassVMIdCache.getLocalData().getOrDefault(
          classIdentifier,
          outputClassVMIdCache.getRemoteData().getOrDefault(
            classIdentifier,
            null
          )
        );
    }

    public UUID getRemoteVersionedMappableIdForClassFromOutput(
      final String className
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        loadVersionedMappableClassCacheForOutput(outputClassVMIdCache,
          "VersionedMappableId from Class by Output",
          OUTPUT_CLASS_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT
        );

        return outputClassVMIdCache.getRemoteData().getOrDefault(
          classIdentifier,
          null
        );
    }

    public void addClass(
      final String className,
      final UUID mappableId,
      final UUID versionedMappableId
    ) {
        final ClassIdentifier classIdentifier = new ClassIdentifier(className);

        outputClassMIdCache.getLocalData().put(
          classIdentifier,
          mappableId
        );
        outputClassVMIdCache.getLocalData().put(
          classIdentifier,
          versionedMappableId
        );
    }

    private void loadMappableClassCacheForOutput(
      final CacheEntry<Map<ClassIdentifier, UUID>> outputClassMIdCache,
      final String name,
      final String queryFormat)
    {
        if (outputClassMIdCache.needsLoading())
        {
            RunCacheLoading(name, (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(queryFormat, buildMappingIdFilter());
                final Map<ClassIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(ClassMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(ClassMappableData::toIdentifier, ClassMappableData::getMappableId)
                    .block();

                outputClassMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    private void loadVersionedMappableClassCacheForOutput(
      final CacheEntry<Map<ClassIdentifier, UUID>> outputClassVMIdCache,
      final String name,
      final String queryFormat)
    {
        if (outputClassVMIdCache.needsLoading())
        {
            RunCacheLoading(name, (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(queryFormat, buildMappingIdFilter());
                final Map<ClassIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(ClassVersionedMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(ClassVersionedMappableData::toIdentifier, ClassVersionedMappableData::getVersionedMappableId)
                    .block();

                outputClassVMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    public UUID getMappableIdForMethod(
      final String methodName,
      final String descriptor,
      final String className
    ) {
        final MethodIdentifier methodIdentifier = new MethodIdentifier(
          methodName,
          remapDescriptor(descriptor),
          getVersionedMappableIdForClassFromOutput(className)
        );

        loadMappableMethodCacheForOutput();

        return methodMIdCache.getLocalData().getOrDefault(
          methodIdentifier,
          methodMIdCache.getRemoteData().getOrDefault(
            methodIdentifier,
            null
          )
        );
    }

    public UUID getRemoteMappableIdForMethod(
      final String methodName,
      final String descriptor,
      final String className
    ) {
        final MethodIdentifier methodIdentifier = new MethodIdentifier(
          methodName,
          remapDescriptor(descriptor),
          getRemoteVersionedMappableIdForClassFromOutput(className)
        );

        loadMappableMethodCacheForOutput();

        return methodMIdCache.getRemoteData().getOrDefault(
          methodIdentifier,
          null
        );
    }

    public UUID getVersionedMappableIdForMethod(
      final String methodName,
      final String descriptor,
      final String className
    ) {
        final MethodIdentifier methodIdentifier = new MethodIdentifier(
          methodName,
          remapDescriptor(descriptor),
          getVersionedMappableIdForClassFromOutput(className)
          );

        loadVersionedMappableMethodCacheForOutput();

        return methodVMIdCache.getLocalData().getOrDefault(
          methodIdentifier,
          methodVMIdCache.getRemoteData().getOrDefault(
            methodIdentifier,
            null
          )
        );
    }

    public UUID getRemoteVersionedMappableIdForMethod(
      final String methodName,
      final String descriptor,
      final String className
    ) {
        final MethodIdentifier methodIdentifier = new MethodIdentifier(
          methodName,
          remapDescriptor(descriptor),
          getRemoteVersionedMappableIdForClassFromOutput(className)
        );

        loadVersionedMappableMethodCacheForOutput();

        return methodVMIdCache.getRemoteData().getOrDefault(
          methodIdentifier,
          null
        );
    }

    public void addMethod(
      final String methodName,
      final String descriptor,
      final UUID parentClassId,
      final UUID mappableId,
      final UUID versionedMappableId
    ) {
        final MethodIdentifier methodIdentifier = new MethodIdentifier(
          methodName,
          remapDescriptor(descriptor),
          parentClassId
        );

        methodMIdCache.getLocalData().put(
          methodIdentifier,
          mappableId
        );
        methodVMIdCache.getLocalData().put(
          methodIdentifier,
          versionedMappableId
        );
    }

    private void loadMappableMethodCacheForOutput()
    {
        if (methodMIdCache.needsLoading())
        {
            RunCacheLoading("MappableId from Method by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(METHOD_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<MethodIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(MethodMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(methodVersionedMappableData -> methodVersionedMappableData.remap(this::remapDescriptor), MethodMappableData::getMappableId)
                    .block();

                this.methodMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    private void loadVersionedMappableMethodCacheForOutput()
    {
        if (methodVMIdCache.needsLoading())
        {
            RunCacheLoading("VersionedMappableId from Method by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(METHOD_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<MethodIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(MethodVersionedMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(methodVersionedMappableData -> methodVersionedMappableData.remap(this::remapDescriptor), MethodVersionedMappableData::getVersionedMappableId)
                    .block();

                this.methodVMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    public UUID getMappableIdForField(
      final String fieldName,
      final String type,
      final String className
    ) {
        final FieldIdentifier fieldIdentifier = new FieldIdentifier(
          fieldName,
          remapType(type),
          getVersionedMappableIdForClassFromOutput(className)
        );

        loadMappableFieldCacheForOutput();

        return fieldMIdCache.getLocalData().getOrDefault(
          fieldIdentifier,
          fieldMIdCache.getRemoteData().getOrDefault(
            fieldIdentifier,
            null
          )
        );
    }

    public UUID getRemoteMappableIdForField(
      final String fieldName,
      final String type,
      final String className
    ) {
        final FieldIdentifier fieldIdentifier = new FieldIdentifier(
          fieldName,
          remapType(type),
          getRemoteVersionedMappableIdForClassFromOutput(className)
        );

        loadMappableFieldCacheForOutput();

        return fieldMIdCache.getRemoteData().getOrDefault(
          fieldIdentifier,
          null
        );
    }

    public UUID getVersionedMappableIdForField(
      final String fieldName,
      final String descriptor,
      final String className
    ) {
        final FieldIdentifier fieldIdentifier = new FieldIdentifier(
          fieldName,
          remapDescriptor(descriptor),
          getVersionedMappableIdForClassFromOutput(className)
        );

        loadVersionedMappableFieldCacheForOutput();

        return fieldVMIdCache.getLocalData().getOrDefault(
          fieldIdentifier,
          fieldVMIdCache.getRemoteData().getOrDefault(
            fieldIdentifier,
            null
          )
        );
    }

    public UUID getRemoteVersionedMappableIdForField(
      final String fieldName,
      final String descriptor,
      final String className
    ) {
        final FieldIdentifier fieldIdentifier = new FieldIdentifier(
          fieldName,
          remapDescriptor(descriptor),
          getRemoteVersionedMappableIdForClassFromOutput(className)
        );

        loadVersionedMappableFieldCacheForOutput();

        return fieldVMIdCache.getRemoteData().getOrDefault(
          fieldIdentifier,
          null
        );
    }

    public void addField(
      final String fieldName,
      final String type,
      final UUID parentClassId,
      final UUID mappableId,
      final UUID versionedMappableId
    )
    {
        final FieldIdentifier fieldIdentifier = new FieldIdentifier(
          fieldName,
          remapType(type),
          parentClassId
        );

        fieldMIdCache.getLocalData().put(
          fieldIdentifier,
          mappableId
        );
        fieldVMIdCache.getLocalData().put(
          fieldIdentifier,
          versionedMappableId
        );
    }

    private void loadMappableFieldCacheForOutput()
    {
        if (fieldMIdCache.needsLoading())
        {
            RunCacheLoading("MappableId from Field by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(FIELD_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<FieldIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(FieldMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(fieldVersionedMappableData -> fieldVersionedMappableData.remap(this::remapType), FieldMappableData::getMappableId)
                    .block();

                this.fieldMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    private void loadVersionedMappableFieldCacheForOutput()
    {
        if (fieldVMIdCache.needsLoading())
        {
            RunCacheLoading("VersionedMappableId from Field by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(FIELD_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<FieldIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(FieldVersionedMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(fieldVersionedMappableData -> fieldVersionedMappableData.remap(this::remapType), FieldVersionedMappableData::getVersionedMappableId)
                    .block();

                this.fieldVMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    public UUID getMappableIdForParameter(
      final String parameterName,
      final String className,
      final String methodName,
      final String descriptor,
      final String type)
    {
        final ParameterIdentifier parameterIdentifier = new ParameterIdentifier(
          parameterName, type, getVersionedMappableIdForClassFromOutput(className), getVersionedMappableIdForMethod(methodName, descriptor, className)
        ).remap(this::remapType);

        loadMappableParameterCacheForOutput();

        return parameterMIdCache.getLocalData().getOrDefault(
          parameterIdentifier,
          parameterMIdCache.getRemoteData().getOrDefault(
            parameterIdentifier,
            null
          )
        );
    }

    public UUID getRemoteMappableIdForParameter(
      final String parameterName,
      final String className,
      final String methodName,
      final String descriptor,
      final String type)
    {
        final ParameterIdentifier parameterIdentifier = new ParameterIdentifier(
          parameterName, type, getRemoteVersionedMappableIdForClassFromOutput(className), getRemoteVersionedMappableIdForMethod(methodName, descriptor, className)
        ).remap(this::remapType);

        loadMappableParameterCacheForOutput();

        return parameterMIdCache.getRemoteData().getOrDefault(
          parameterIdentifier,
          null
        );
    }

    public UUID getVersionedMappableIdForParameter(
      final String parameterName,
      final String className,
      final String methodName,
      final String descriptor,
      final String type)
    {
        final ParameterIdentifier parameterIdentifier = new ParameterIdentifier(
          parameterName, type, getVersionedMappableIdForClassFromOutput(className), getVersionedMappableIdForMethod(methodName, descriptor, className)
        ).remap(this::remapType);

        loadVersionedMappableParameterCacheForOutput();

        return parameterVMIdCache.getLocalData().getOrDefault(
          parameterIdentifier,
          parameterVMIdCache.getRemoteData().getOrDefault(
            parameterIdentifier,
            null
          )
        );
    }

    public UUID getRemoteVersionedMappableIdForParameter(
      final String parameterName,
      final String className,
      final String methodName,
      final String descriptor,
      final String type)
    {
        final ParameterIdentifier parameterIdentifier = new ParameterIdentifier(
          parameterName, type, getRemoteVersionedMappableIdForClassFromOutput(className), getRemoteVersionedMappableIdForMethod(methodName, descriptor, className)
        ).remap(this::remapType);

        loadVersionedMappableParameterCacheForOutput();

        return parameterVMIdCache.getRemoteData().getOrDefault(
          parameterIdentifier,
          null
        );
    }

    public void addParameter(
      final String parameterName,
      final UUID parentClassId,
      final UUID parentMethodId,
      final String type,
      final UUID mappableId,
      final UUID versionedMappableId
    ) {
        final ParameterIdentifier parameterIdentifier = new ParameterIdentifier(
          parameterName, type, parentClassId, parentMethodId
        ).remap(this::remapType);

        parameterMIdCache.getLocalData().put(
          parameterIdentifier,
          mappableId
        );
        parameterVMIdCache.getLocalData().put(
          parameterIdentifier,
          versionedMappableId
        );
    }

    private void loadMappableParameterCacheForOutput()
    {
        if (parameterMIdCache.needsLoading())
        {
            RunCacheLoading("MappableId from Parameter by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(PARAMETER_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<ParameterIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(ParameterMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(parameterMappableData -> parameterMappableData.remap(this::remapType), ParameterMappableData::getMappableId)
                    .block();

                this.parameterMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    private void loadVersionedMappableParameterCacheForOutput()
    {
        if (parameterVMIdCache.needsLoading())
        {
            RunCacheLoading("VersionedMappableId from Parameter by Output", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(PARAMETER_VERSIONED_MAPPABLE_ID_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<ParameterIdentifier, UUID> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(ParameterVersionedMappableData.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> {
                        dbLoad.stop();
                        return data;
                    })
                    .flatMapIterable(Function.identity())
                    .collectMap(parameterVersionedMappableData -> parameterVersionedMappableData.remap(this::remapType), ParameterVersionedMappableData::getVersionedMappableId)
                    .block();

                this.parameterVMIdCache.load(remoteData);

                fullLoad.stop();
            });
        }
    }

    public GameVersionDMO getGameVersionFromName(
        final String gameVersionName
    ) {
        if (namedGameVersionCache.needsLoading())
        {
            RunCacheLoading("Named GameVersion", (fullLoad, dbLoad) -> {
                final String classLoadQuery = GAMEVERSION_LOAD_QUERY_FORMAT;
                final Map<String, GameVersionDMO> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(GameVersionDMO.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> { dbLoad.stop(); return data;})
                    .flatMapIterable(Function.identity())
                    .collectMap(GameVersionDMO::getName)
                    .block();

                this.namedGameVersionCache.load(remoteData);

                fullLoad.stop();
            });
        }

        return namedGameVersionCache.getLocalData().getOrDefault(
          gameVersionName,
          namedGameVersionCache.getRemoteData().getOrDefault(
            gameVersionName,
            null
          )
        );
    }

    public void addGameVersion(
      final GameVersionDMO gameVersion
    )
    {
        namedGameVersionCache.getLocalData().put(gameVersion.getName(), gameVersion);
    }

    public ReleaseDMO getRelease(
      final UUID mappingTypeId,
      final String releaseName
    ) {
        final ReleaseIdentifier releaseIdentifier = new ReleaseIdentifier(
          mappingTypeId, releaseName
        );

        if (releaseCache.needsLoading())
        {
            RunCacheLoading("Release", (fullLoad, dbLoad) -> {
                final String classLoadQuery = String.format(RELEASE_LOAD_QUERY_FORMAT, buildMappingIdFilter());
                final Map<ReleaseIdentifier, ReleaseDMO> remoteData =
                  this.r2dbcEntityTemplate.getDatabaseClient().sql(classLoadQuery)
                    .map(new EntityRowMapper<>(ReleaseDMO.class, this.r2dbcEntityTemplate.getConverter()))
                    .all()
                    .collectList()
                    .map(data -> { dbLoad.stop(); return data;})
                    .flatMapIterable(Function.identity())
                    .collectMap(releaseDMO -> new ReleaseIdentifier(releaseDMO.getMappingTypeId(), releaseDMO.getName()))
                    .block();

                this.releaseCache.load(remoteData);

                fullLoad.stop();
            });
        }

        return releaseCache.getLocalData().getOrDefault(
          releaseIdentifier,
          releaseCache.getRemoteData().getOrDefault(
            releaseIdentifier,
            null
          )
        );
    }

    public void addRelease(
      final ReleaseDMO release
    ) {
        releaseCache.getLocalData().put(
          new ReleaseIdentifier(release.getMappingTypeId(), release.getName()),
          release
        );
    }

    public String remapDescriptor(
      final String descriptor
    ) {
        final MethodDesc desc = new MethodDesc(descriptor);
        final MethodDesc remappedDesc = desc.remap(
          s -> Optional.ofNullable(
            getMappableIdForClassFromInput(s)
          ).map(UUID::toString)
        );

        return remappedDesc.toString();
    }
    
    public String remapType(
      final String type
    ) {
        final RemappableType remappableType = new RemappableType(type);
        final RemappableType remappedType = remappableType.remap(
          s -> Optional.ofNullable(
            getMappableIdForClassFromInput(s)
          ).map(UUID::toString)
        );
        
        return remappedType.getType();
    }

    public void destroyCache()
    {
        inputClassMIdCache.clear();
        inputClassVMIdCache.clear();

        outputClassMIdCache.clear();
        outputClassVMIdCache.clear();

        methodMIdCache.clear();
        methodVMIdCache.clear();

        fieldMIdCache.clear();
        fieldVMIdCache.clear();

        parameterMIdCache.clear();
        parameterVMIdCache.clear();

        namedGameVersionCache.clear();
        releaseCache.clear();
    }

    private void RunCacheLoading(final String loadingName, final BiConsumer<Stopwatch, Stopwatch> loader)
    {
        LOGGER.info("Starting loading of the cache for: " + loadingName);
        final Stopwatch fullLoadStopWatch = Stopwatch.createStarted();
        final Stopwatch dbLoadStopWatch = Stopwatch.createStarted();

        loader.accept(fullLoadStopWatch, dbLoadStopWatch);

        LOGGER.info(String.format("Finished loading of the cache for: %s ( %s / %s )", loadingName, fullLoadStopWatch, dbLoadStopWatch));
    }

    private static final class CacheEntry<T> {
        private boolean loaded = false;

        private final Supplier<T> defaultValueSupplier;

        private T remoteData;
        private T localData;

        public CacheEntry(final Supplier<T> defaultValueSupplier)
        {
            this.defaultValueSupplier = defaultValueSupplier;

            this.clear();
        }

        public boolean needsLoading()
        {
            return !loaded;
        }

        public T getRemoteData()
        {
            return remoteData;
        }

        public T getLocalData()
        {
            return localData;
        }

        public void load(T data)
        {
            this.remoteData = data;
            this.loaded = true;
        }

        public void clear()
        {
            this.remoteData = this.defaultValueSupplier.get();
            this.localData = this.defaultValueSupplier.get();
            this.loaded = false;
        }
    }

    public static class ClassIdentifier {
        private final String name;

        private ClassIdentifier(final String name) {this.name = name;}

        public String getName()
        {
            return name;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof ClassIdentifier))
            {
                return false;
            }
            final ClassIdentifier that = (ClassIdentifier) o;
            return Objects.equals(getName(), that.getName());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getName());
        }
    }

    public static class ClassVersionedMappableData extends ClassIdentifier {
        private final UUID versionedMappableId;

        private ClassVersionedMappableData(final String name, final UUID versionedMappableId)
        {
            super(name);
            this.versionedMappableId = versionedMappableId;
        }

        public ClassIdentifier toIdentifier()
        {
            return new ClassIdentifier(getName());
        }

        public UUID getVersionedMappableId()
        {
            return versionedMappableId;
        }
    }

    public static class ClassMappableData extends ClassIdentifier {
        private final UUID mappableId;

        private ClassMappableData(final String name, final UUID mappableId)
        {
            super(name);
            this.mappableId = mappableId;
        }

        public ClassIdentifier toIdentifier()
        {
            return new ClassIdentifier(getName());
        }

        public UUID getMappableId()
        {
            return mappableId;
        }
    }

    public static class MethodIdentifier {
        private final String name;
        private final String descriptor;
        private final UUID parentClassIdentifier;

        private MethodIdentifier(final String name, final String descriptor, final UUID parentClassIdentifier) {
            this.name = name;
            this.descriptor = descriptor;
            this.parentClassIdentifier = parentClassIdentifier;
        }

        public String getName()
        {
            return name;
        }

        public String getDescriptor()
        {
            return descriptor;
        }

        public UUID getParentClassIdentifier()
        {
            return parentClassIdentifier;
        }

        public MethodIdentifier remap(final Function<String, String> remapper)
        {
            return new MethodIdentifier(
              name,
              remapper.apply(descriptor),
              parentClassIdentifier
            );
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof MethodIdentifier))
            {
                return false;
            }
            final MethodIdentifier that = (MethodIdentifier) o;
            return Objects.equals(getName(), that.getName()) && Objects.equals(getDescriptor(), that.getDescriptor()) && Objects.equals(
              getParentClassIdentifier(),
              that.getParentClassIdentifier());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getName(), getDescriptor(), getParentClassIdentifier());
        }
    }

    public static class MethodVersionedMappableData extends MethodIdentifier {
        private final UUID versionedMappableId;

        private MethodVersionedMappableData(final String name, final String descriptor, final UUID parentClassIdentifier, final UUID versionedMappableId)
        {
            super(name, descriptor, parentClassIdentifier);
            this.versionedMappableId = versionedMappableId;
        }

        public UUID getVersionedMappableId()
        {
            return versionedMappableId;
        }
    }

    public static class MethodMappableData extends MethodIdentifier {
        private final UUID mappableId;

        private MethodMappableData(final String name, final String descriptor, final UUID parentClassIdentifier, final UUID mappableId)
        {
            super(name, descriptor, parentClassIdentifier);
            this.mappableId = mappableId;
        }

        public UUID getMappableId()
        {
            return mappableId;
        }
    }

    public static class FieldIdentifier {
        private final String name;
        private final String type;
        private final UUID   parentClassIdentifier;

        private FieldIdentifier(final String name, final String type, final UUID parentClassIdentifier) {
            this.name = name;
            this.type = type;
            this.parentClassIdentifier = parentClassIdentifier;
        }

        public String getName()
        {
            return name;
        }

        public String getType()
        {
            return type;
        }

        public UUID getParentClassIdentifier()
        {
            return parentClassIdentifier;
        }

        public FieldIdentifier remap(final Function<String, String> remapper)
        {
            return new FieldIdentifier(
              name,
              remapper.apply(type),
              parentClassIdentifier
            );
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof FieldIdentifier))
            {
                return false;
            }
            final FieldIdentifier that = (FieldIdentifier) o;
            return Objects.equals(getName(), that.getName()) && Objects.equals(getType(), that.getType()) && Objects.equals(getParentClassIdentifier(),
              that.getParentClassIdentifier());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getName(), getType(), getParentClassIdentifier());
        }
    }

    public static class FieldVersionedMappableData extends FieldIdentifier {
        private final UUID versionedMappableId;

        private FieldVersionedMappableData(final String name, final String type, final UUID parentClassIdentifier, final UUID versionedMappableId)
        {
            super(name, type, parentClassIdentifier);
            this.versionedMappableId = versionedMappableId;
        }

        public UUID getVersionedMappableId()
        {
            return versionedMappableId;
        }
    }

    public static class FieldMappableData extends FieldIdentifier {
        private final UUID mappableId;

        private FieldMappableData(final String name, final String type, final UUID parentClassIdentifier, final UUID mappableId)
        {
            super(name, type, parentClassIdentifier);
            this.mappableId = mappableId;
        }

        public UUID getMappableId()
        {
            return mappableId;
        }
    }

    public static class ParameterIdentifier {
        private final String name;
        private final String type;
        private final UUID   parentClassIdentifier;
        private final UUID   parentMethodIdentifier;

        private ParameterIdentifier(final String name, final String type, final UUID parentClassIdentifier, final UUID parentMethodIdentifier) {
            this.name = name;
            this.type = type;
            this.parentClassIdentifier = parentClassIdentifier;
            this.parentMethodIdentifier = parentMethodIdentifier;
        }

        public String getName()
        {
            return name;
        }

        public String getType()
        {
            return type;
        }

        public UUID getParentClassIdentifier()
        {
            return parentClassIdentifier;
        }

        public UUID getParentMethodIdentifier()
        {
            return parentMethodIdentifier;
        }

        public ParameterIdentifier remap(final Function<String, String> remapper)
        {
            return new ParameterIdentifier(
              name,
              remapper.apply(type),
              parentClassIdentifier,
              parentMethodIdentifier
            );
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof ParameterIdentifier))
            {
                return false;
            }
            final ParameterIdentifier that = (ParameterIdentifier) o;
            return Objects.equals(getName(), that.getName()) && Objects.equals(getType(), that.getType()) && Objects.equals(getParentClassIdentifier(),
              that.getParentClassIdentifier()) && Objects.equals(getParentMethodIdentifier(), that.getParentMethodIdentifier());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getName(), getType(), getParentClassIdentifier(), getParentMethodIdentifier());
        }
    }

    public static class ParameterVersionedMappableData extends ParameterIdentifier {

        private final UUID versionedMappableId;

        private ParameterVersionedMappableData(
          final String name,
          final String type,
          final UUID parentClassIdentifier,
          final UUID parentMethodIdentifier,
          final UUID versionedMappableId)
        {
            super(name, type, parentClassIdentifier, parentMethodIdentifier);
            this.versionedMappableId = versionedMappableId;
        }

        public UUID getVersionedMappableId()
        {
            return versionedMappableId;
        }
    }

    public static class ParameterMappableData extends ParameterIdentifier {

        private final UUID mappableId;

        private ParameterMappableData(
          final String name,
          final String type,
          final UUID parentClassIdentifier,
          final UUID parentMethodIdentifier,
          final UUID mappableId)
        {
            super(name, type, parentClassIdentifier, parentMethodIdentifier);
            this.mappableId = mappableId;
        }

        public UUID getMappableId()
        {
            return mappableId;
        }
    }

    public static class ReleaseIdentifier {
        private final UUID mappingTypeId;
        private final String name;

        private ReleaseIdentifier(final UUID mappingTypeId, final String name) {
            this.mappingTypeId = mappingTypeId;
            this.name = name;
        }

        public UUID getMappingTypeId()
        {
            return mappingTypeId;
        }

        public String getName()
        {
            return name;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof ReleaseIdentifier))
            {
                return false;
            }
            final ReleaseIdentifier that = (ReleaseIdentifier) o;
            return Objects.equals(getMappingTypeId(), that.getMappingTypeId()) && Objects.equals(getName(), that.getName());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getMappingTypeId(), getName());
        }
    }
}
