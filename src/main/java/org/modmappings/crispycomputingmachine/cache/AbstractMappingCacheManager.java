package org.modmappings.crispycomputingmachine.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MappingKey;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractMappingCacheManager {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final DatabaseClient databaseClient;

    private Map<MappingKey, MappingCacheEntry> outputCache = new HashMap<>();
    private Map<MappingKey, MappingCacheEntry> inputCache = new HashMap<>();
    private Map<UUID, MappableDMO> mappableCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdClassCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdMethodCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdFieldCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdParameterCache = new HashMap<>();
    private Map<UUID, GameVersionDMO> gameVersionIdCache = new HashMap<>();
    private Map<String, GameVersionDMO> gameVersionNameCache = new HashMap<>();
    private Map<UUID, ReleaseDMO> releaseIdCache = new HashMap<>();
    private Map<Tuple2<UUID, String>, ReleaseDMO> releaseNameCache = new HashMap<>();

    protected AbstractMappingCacheManager(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public void initializeCache() {
        final List<UUID> mappingTypeIds = getMappingTypeIds();

        LOGGER.warn("Rebuilding cache for: " + mappingTypeIds);

        if (mappingTypeIds.isEmpty())
            return;

        final String mappingTypeFilterQueryComponent =
                mappingTypeIds.stream()
                        .map(id -> "m.mapping_type_id = '" + id + "'")
                        .reduce((left, right) -> left + " or " + right).get();

/*        this.outputCache = databaseClient.execute(String.format(
                "SELECT m.input as input, m.output as output, mp.id as mappable_id, vm.id as versioned_mappable_id, mp.type as mappable_type, pcm.output as parent_class_output, pmm.output as parent_method_output, gv.id as game_version_id, gv.name as game_version_name, vm.type as type, vm.descriptor as descriptor FROM mapping m\n" +
                        "JOIN release_component rc on m.id = rc.mapping_id\n" +
                        "JOIN versioned_mappable vm on m.versioned_mappable_id = vm.id\n" +
                        "JOIN game_version gv on vm.game_version_id = gv.id\n" +
                        "JOIN mappable mp on vm.mappable_id = mp.id\n" +
                        "JOIN mapping_type mt on m.mapping_type_id = mt.id\n" +
                        "LEFT OUTER JOIN mapping m2 ON m.versioned_mappable_id = m2.versioned_mappable_id And m.mapping_type_id = m2.mapping_type_id and m.created_on < m2.created_on\n" +
                        "LEFT OUTER JOIN mapping pcm ON vm.parent_class_id = pcm.versioned_mappable_id and pcm.mapping_type_id = mt.id\n" +
                        "LEFT OUTER JOIN mapping pmm ON vm.parent_method_id = pmm.versioned_mappable_id and pmm.mapping_type_id = mt.id\n" +
                        "WHERE m2.id is null and (%s)",
                mappingTypeFilterQueryComponent)
        )*/
        final Set<MappingCacheEntry> cacheEntries = databaseClient.execute(String.format("SELECT DISTINCT ON (m.versioned_mappable_id)  m.input as input, m.output as output, mp.id as mappable_id, vm.id as versioned_mappable_id, mp.type as mappable_type, pcm.output as parent_class_output, pmm.output as parent_method_output, gv.id as game_version_id, gv.name as game_version_name, vm.type as type, vm.descriptor as descriptor from mapping m\n" +
                        "    JOIN versioned_mappable vm on m.versioned_mappable_id = vm.id\n" +
                        "    JOIN game_version gv on vm.game_version_id = gv.id\n" +
                        "    JOIN mappable mp on vm.mappable_id = mp.id\n" +
                        "    LEFT OUTER JOIN mapping pcm ON vm.parent_class_id = pcm.versioned_mappable_id and pcm.mapping_type_id = m.mapping_type_id\n" +
                        "    LEFT OUTER JOIN mapping pmm ON vm.parent_method_id = pmm.versioned_mappable_id and pmm.id = m.mapping_type_id\n" +
                        "where (%s)\n" +
                        "order by m.versioned_mappable_id, m.created_on desc;",
                mappingTypeFilterQueryComponent))
                .as(MappingCacheEntry.class)
                .fetch()
                .all()
                .collect(Collectors.toSet())
                .block();

        this.outputCache = cacheEntries.stream()
                .collect(Collectors.toMap(
                        mce -> new MappingKey(mce.getOutput(), mce.getMappableType(), mce.getParentClassOutput(), mce.getParentMethodOutput(), mce.getType(), mce.getDescriptor()), Function.identity()
                ));

        this.inputCache = cacheEntries.stream()
                .collect(Collectors.toMap(
                        mce -> new MappingKey(mce.getInput(), mce.getMappableType(), mce.getParentClassOutput(), mce.getParentMethodOutput(), mce.getType(), mce.getDescriptor()), Function.identity()
                ));

        this.mappableCache = this.databaseClient.select()
                .from(MappableDMO.class)
                .fetch()
                .all()
                .collectMap(MappableDMO::getId)
                .block();

        this.versionedMappableIdClassCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableType() == MappableTypeDMO.CLASS)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdMethodCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableType() == MappableTypeDMO.METHOD)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdFieldCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableType() == MappableTypeDMO.FIELD)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdFieldCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableType() == MappableTypeDMO.PARAMETER)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.gameVersionIdCache = this.databaseClient.select().from(GameVersionDMO.class).fetch().all()
                .collectMap(GameVersionDMO::getId)
                .block();

        this.gameVersionNameCache = this.gameVersionIdCache.values().stream()
                .collect(Collectors.toMap(GameVersionDMO::getName, Function.identity()));

        final String mappingTypeReleaseQueryComponent =
                mappingTypeIds.stream()
                        .map(id -> "release.mapping_type_id = '" + id + "'")
                        .reduce((left, right) -> left + " or " + right).get();


        final List<ReleaseDMO> releaseDMOS = this.databaseClient.execute(
                "SELECT release.id, release.created_by, release.created_on, release.name, release.game_version_id, release.mapping_type_id, release.is_snapshot, release.state " +
                        "FROM release " +
                        "WHERE (" + mappingTypeReleaseQueryComponent + ")")
                .as(ReleaseDMO.class)
                .fetch()
                .all()
                .collectList()
                .block();

        this.releaseIdCache = releaseDMOS
                .stream()
                .collect(Collectors.toMap(ReleaseDMO::getId, Function.identity()));

        this.releaseNameCache = this.releaseIdCache.values().stream()
                .collect(Collectors.toMap(r -> Tuples.of(r.getMappingTypeId(), r.getName()), Function.identity()));

        LOGGER.warn("Rebuilding cache for: " + mappingTypeIds + " completed.");
    }

    protected abstract List<UUID> getMappingTypeIds();

    public void destroyCache() {
        this.outputCache = Collections.emptyMap();
        this.versionedMappableIdClassCache = Collections.emptyMap();
        this.versionedMappableIdMethodCache = Collections.emptyMap();
        this.versionedMappableIdFieldCache = Collections.emptyMap();
        this.gameVersionIdCache = Collections.emptyMap();
        this.gameVersionNameCache = Collections.emptyMap();
    }

    public GameVersionDMO getGameVersion(UUID id)
    {
        return this.gameVersionIdCache.get(id);
    }

    public GameVersionDMO getGameVersion(String name)
    {
        return this.gameVersionNameCache.get(name);
    }

    public ReleaseDMO getRelease(UUID id)
    {
        return this.releaseIdCache.get(id);
    }

    public ReleaseDMO getRelease(UUID mappingType, String name)
    {
        return this.releaseNameCache.get(Tuples.of(mappingType, name));
    }

    public MappableDMO getMappable(UUID id) { return this.mappableCache.get(id); }

    public MappingCacheEntry getClassViaOutput(String mapping) {
        String parent = null;
        if (mapping.contains("$"))
            parent = mapping.substring(0, mapping.lastIndexOf("$"));


        final MappingKey id = new MappingKey(mapping, MappableTypeDMO.CLASS, parent, null, null, null);
        final MappingCacheEntry clz = this.outputCache.get(id);
        if (clz == null)
            return null;

        return clz;
    }

    public MappingCacheEntry getMethodViaOutput(String mapping, String parentClass, String descriptor) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.METHOD,
                parentClass,
                null,
                null,
                descriptor
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getFieldViaOutput(String mapping, String parentClass, String type) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.FIELD,
                parentClass,
                null,
                type,
                null
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getParameterViaOutput(String mapping, String parentClass, String parentMethod, String type) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.PARAMETER,
                parentClass,
                parentMethod,
                type,
                null
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getClassViaInput(String mapping) {
        String parent = null;
        if (mapping.contains("$")) {
            parent = mapping.substring(0, mapping.lastIndexOf("$"));
            parent = getClassViaInput(parent).getOutput();
        }

        final MappingKey id = new MappingKey(mapping, MappableTypeDMO.CLASS, parent, null, null, null);
        final MappingCacheEntry clz = this.inputCache.get(id);
        if (clz == null)
            return null;

        return clz;
    }

    public MappingCacheEntry getMethodViaInput(String mapping, String parentClass, String descriptor) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.METHOD,
                parentClass,
                null,
                null,
                descriptor
        );
        return this.inputCache.get(id);
    }

    public MappingCacheEntry getFieldViaInput(String mapping, String parentClass, String type) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.FIELD,
                parentClass,
                null,
                type,
                null
        );
        return this.inputCache.get(id);
    }

    public MappingCacheEntry getParameterViaInput(String mapping, String parentClass, String parentMethod, String type) {
        final MappingKey id = new MappingKey(
                mapping,
                MappableTypeDMO.PARAMETER,
                parentClass,
                parentMethod,
                type,
                null
        );
        return this.inputCache.get(id);
    }
    
    public void registerNewGameVersion(final GameVersionDMO gameVersion) {
        this.gameVersionIdCache.put(gameVersion.getId(), gameVersion);
        this.gameVersionNameCache.put(gameVersion.getName(), gameVersion);
    }

    public void registerNewRelease(final ReleaseDMO release)
    {
        this.releaseIdCache.put(release.getId(), release);
        this.releaseNameCache.put(Tuples.of(release.getMappingTypeId(), release.getName()), release);
    }

    public void registerNewClass(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getInput(),
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.CLASS,
                versionedMappable.getParentClassId() != null && this.versionedMappableIdClassCache.containsKey(versionedMappable.getParentClassId()) ? this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput() : null,
                null,
                versionedMappable.getGameVersionId(),
                gameVersionIdCache.get(versionedMappable.getGameVersionId()).getName(),
                null, null);

        final MappingKey inputMappingKey = new MappingKey(
                newEntry.getInput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        final MappingKey outputMappingKey = new MappingKey(
                newEntry.getOutput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdClassCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewMethod(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getInput(),
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.METHOD,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                null,
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName(),
                null,
                versionedMappable.getDescriptor());

        final MappingKey inputMappingKey = new MappingKey(
                newEntry.getInput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        final MappingKey outputMappingKey = new MappingKey(
                newEntry.getOutput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdMethodCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewField(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getInput(),
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.FIELD,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                null,
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName(),
                versionedMappable.getType(), null);

        final MappingKey inputMappingKey = new MappingKey(
                newEntry.getInput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        final MappingKey outputMappingKey = new MappingKey(
                newEntry.getOutput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdFieldCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewParameter(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getInput(),
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.PARAMETER,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                this.versionedMappableIdMethodCache.get(versionedMappable.getParentMethodId()).getOutput(),
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName(),
                versionedMappable.getType(), null);

        final MappingKey inputMappingKey = new MappingKey(
                newEntry.getInput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        final MappingKey outputMappingKey = new MappingKey(
                newEntry.getOutput(),
                newEntry.getMappableType(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput(),
                newEntry.getType(),
                newEntry.getDescriptor()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdParameterCache.put(versionedMappable.getId(), newEntry);
    }

    protected UUID getOrCreateIdForMappingType(
            final String mappingTypeName,
            final boolean isVisible,
            final boolean isEditable,
            final String stateIn,
            final String stateOut
    )
    {
        return databaseClient.select()
                .from(MappingTypeDMO.class)
                .matching(Criteria.where("name").is(mappingTypeName))
                .fetch()
                .first()
                .switchIfEmpty(
                        Mono.just(new MappingTypeDMO(UUID.randomUUID(), Constants.SYSTEM_ID, Timestamp.from(Instant.now()), mappingTypeName, isVisible, isEditable, stateIn, stateOut))
                                .flatMap(mappingType -> databaseClient.insert()
                                        .into(MappingTypeDMO.class)
                                        .using(mappingType)
                                        .fetch()
                                        .first()
                                        .map(r -> mappingType))
                )
                .map(MappingTypeDMO::getId).blockOptional().orElse(null);
    }
}
