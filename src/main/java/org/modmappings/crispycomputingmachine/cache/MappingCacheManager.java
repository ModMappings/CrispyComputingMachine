package org.modmappings.crispycomputingmachine.cache;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.data.r2dbc.core.DatabaseClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingCacheManager {

    private final DatabaseClient databaseClient;

    private Map<FullCacheId, MappingCacheEntry> outputCache = new HashMap<>();
    private Map<UUID, MappableDMO> mappableCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdClassCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdMethodCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdFieldCache = new HashMap<>();
    private Map<UUID, MappingCacheEntry> versionedMappableIdParameterCache = new HashMap<>();
    private Map<UUID, GameVersionDMO> gameVersionIdCache = new HashMap<>();
    private Map<String, GameVersionDMO> gameVersionNameCache = new HashMap<>();

    public MappingCacheManager(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public void initializeCache() {
        this.outputCache = databaseClient.execute("SELECT m.output as output, mp.id as mappable_id, mp.type as mappable_type, pcm.output as parent_class_output, pmm.output as parent_method_output, gv.id as game_version_id, gv.name as game_version_name FROM mapping m\n" +
                "    JOIN release_component rc on m.id = rc.mapping_id\n" +
                "    JOIN versioned_mappable vm on m.versioned_mappable_id = vm.id\n" +
                "    JOIN game_version gv on vm.game_version_id = gv.id\n" +
                "    JOIN mappable mp on vm.mappable_id = mp.id\n" +
                "    JOIN mapping_type mt on m.mapping_type_id = mt.id\n" +
                "    LEFT OUTER JOIN mapping m2 ON m.versioned_mappable_id = m2.versioned_mappable_id And m.mapping_type_id = m2.mapping_type_id and m.created_on < m2.created_on\n" +
                "    LEFT OUTER JOIN mapping pcm ON vm.parent_class_id = pcm.versioned_mappable_id\n" +
                "    LEFT OUTER JOIN mapping pmm ON vm.parent_method_id = pmm.versioned_mappable_id\n" +
                "WHERE m2.id is null")
                .as(MappingCacheEntry.class)
                .fetch()
                .all()
                .collectMap(mce -> new FullCacheId(mce.getOutput(), mce.getMappableTypeDMO(), mce.getParentClassOutput(), mce.getParentMethodOutput()), Function.identity())
                .block();

        this.mappableCache = this.databaseClient.select()
                .from(MappableDMO.class)
                .fetch()
                .all()
                .collectMap(MappableDMO::getId)
                .block();

        this.versionedMappableIdClassCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableTypeDMO() == MappableTypeDMO.CLASS)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdMethodCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableTypeDMO() == MappableTypeDMO.METHOD)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdFieldCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableTypeDMO() == MappableTypeDMO.FIELD)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdFieldCache = this.outputCache.values().stream()
                .filter(mce -> mce.getMappableTypeDMO() == MappableTypeDMO.PARAMETER)
                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.gameVersionIdCache = this.databaseClient.select().from(GameVersionDMO.class).fetch().all()
                .collectMap(GameVersionDMO::getId)
                .block();

        this.gameVersionNameCache = this.gameVersionIdCache.values().stream()
                .collect(Collectors.toMap(GameVersionDMO::getName, Function.identity()));
    }

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

    public MappingCacheEntry getClass(String mapping) {
        String parent = null;
        if (mapping.contains("$"))
            parent = mapping.substring(0, mapping.lastIndexOf("$"));

        final FullCacheId id = new FullCacheId(mapping, MappableTypeDMO.CLASS, parent, null);
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getMethod(String mapping, String parentClass) {
        final FullCacheId id = new FullCacheId(
                mapping,
                MappableTypeDMO.METHOD,
                parentClass,
                null
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getField(String mapping, String parentClass) {
        final FullCacheId id = new FullCacheId(
                mapping,
                MappableTypeDMO.FIELD,
                parentClass,
                null
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getParameter(String mapping, String parentClass, String parentMethod) {
        final FullCacheId id = new FullCacheId(
                mapping,
                MappableTypeDMO.PARAMETER,
                parentClass,
                parentMethod
        );
        return this.outputCache.get(id);
    }

    public void registerNewGameVersion(final GameVersionDMO gameVersion) {
        this.gameVersionIdCache.put(gameVersion.getId(), gameVersion);
    }

    public void registerNewClass(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.CLASS,
                versionedMappable.getParentClassId() != null ? this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput() : null,
                null,
                versionedMappable.getGameVersionId(),
                gameVersionIdCache.get(versionedMappable.getGameVersionId()).getName()
        );

        final FullCacheId fullCacheId = new FullCacheId(
                newEntry.getOutput(),
                newEntry.getMappableTypeDMO(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput()
        );

        this.outputCache.put(fullCacheId, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdClassCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewMethod(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.METHOD,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                null,
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName()
        );

        final FullCacheId fullCacheId = new FullCacheId(
                newEntry.getOutput(),
                newEntry.getMappableTypeDMO(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput()
        );

        this.outputCache.put(fullCacheId, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdMethodCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewField(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.FIELD,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                null,
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName()
        );

        final FullCacheId fullCacheId = new FullCacheId(
                newEntry.getOutput(),
                newEntry.getMappableTypeDMO(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput()
        );

        this.outputCache.put(fullCacheId, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdFieldCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewParameter(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping) {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
                mapping.getOutput(),
                mappable.getId(),
                versionedMappable.getId(),
                MappableTypeDMO.PARAMETER,
                this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
                this.versionedMappableIdMethodCache.get(versionedMappable.getParentMethodId()).getOutput(),
                versionedMappable.getGameVersionId(),
                getGameVersion(versionedMappable.getGameVersionId()).getName()
        );

        final FullCacheId fullCacheId = new FullCacheId(
                newEntry.getOutput(),
                newEntry.getMappableTypeDMO(),
                newEntry.getParentClassOutput(),
                newEntry.getParentMethodOutput()
        );

        this.outputCache.put(fullCacheId, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdParameterCache.put(versionedMappable.getId(), newEntry);
    }

    private static final class FullCacheId {
        private final String mapping;
        private final MappableTypeDMO mappingType;
        private final String parentClassMapping;
        private final String parentMethodMapping;

        private FullCacheId(final String mapping, final MappableTypeDMO mappingType, final String parentClassMapping, final String parentMethodMapping) {
            this.mapping = mapping;
            this.mappingType = mappingType;
            this.parentClassMapping = parentClassMapping;
            this.parentMethodMapping = parentMethodMapping;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final FullCacheId that = (FullCacheId) o;

            if (!mapping.equals(that.mapping)) return false;
            if (!mappingType.equals(that.mappingType)) return false;
            if (!Objects.equals(parentClassMapping, that.parentClassMapping))
                return false;
            return Objects.equals(parentMethodMapping, that.parentMethodMapping);
        }

        @Override
        public int hashCode() {
            int result = mapping.hashCode();
            result = 31 * result + mappingType.hashCode();
            result = 31 * result + (parentClassMapping != null ? parentClassMapping.hashCode() : 0);
            result = 31 * result + (parentMethodMapping != null ? parentMethodMapping.hashCode() : 0);
            return result;
        }
    }
}