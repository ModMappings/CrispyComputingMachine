package org.modmappings.crispycomputingmachine.cache;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.srgutils.MinecraftVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.utils.*;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.modmappings.crispycomputingmachine.utils.StreamUtils.distinctByKey;

public abstract class AbstractMappingCacheManager
{

    private static final Logger LOGGER = LogManager.getLogger();

    protected final DatabaseClient databaseClient;

    private AbstractMappingCacheManager remappingManager = this;

    private Map<MappingKey, MappingCacheEntry>    outputCache                       = new HashMap<>();
    private Map<MappingKey, MappingCacheEntry>    inputCache                        = new HashMap<>();
    private Map<UUID, MappableDMO>                mappableCache                     = new HashMap<>();
    private Map<UUID, MappingCacheEntry>          versionedMappableIdClassCache     = new HashMap<>();
    private Map<UUID, MappingCacheEntry>          versionedMappableIdMethodCache    = new HashMap<>();
    private Map<UUID, MappingCacheEntry>          versionedMappableIdFieldCache     = new HashMap<>();
    private Map<UUID, MappingCacheEntry>          versionedMappableIdParameterCache = new HashMap<>();
    private Map<String, List<MappingCacheEntry>>  classMembers                      = new HashMap<>();
    private Map<UUID, GameVersionDMO>             gameVersionIdCache                = new HashMap<>();
    private Map<String, GameVersionDMO>           gameVersionNameCache              = new HashMap<>();
    private Map<UUID, ReleaseDMO>                 releaseIdCache                    = new HashMap<>();
    private Map<Tuple2<UUID, String>, ReleaseDMO> releaseNameCache                  = new HashMap<>();
    private Map<String, Map<UUID, List<UUID>>>    superTypeCache                    = new HashMap<>();
    private Map<String, Map<UUID, List<UUID>>>    overridesMethodsCache             = new HashMap<>();

    protected AbstractMappingCacheManager(final DatabaseClient databaseClient)
    {
        this.databaseClient = databaseClient;
    }

    public void initializeCache()
    {
        final List<UUID> mappingTypeIds = getMappingTypeIds();

        LOGGER.warn("Rebuilding cache for: " + mappingTypeIds);

        if (mappingTypeIds.isEmpty())
        {
            return;
        }

        LOGGER.info("Setting up global game versions for: " + mappingTypeIds);
        this.gameVersionIdCache = this.databaseClient.select().from(GameVersionDMO.class).fetch().all()
                                    .collectMap(GameVersionDMO::getId)
                                    .block();

        this.gameVersionNameCache = this.gameVersionIdCache.values().stream()
                                      .collect(Collectors.toMap(GameVersionDMO::getName, Function.identity()));

        LOGGER.info("Building master caches for: " + mappingTypeIds);
        this.outputCache = getCacheFromDatabase(false).parallelStream()
                             .collect(Collectors.toMap(
                               mce -> new MappingKey(mce.getOutput(),
                                 mce.getMappableType(),
                                 mce.getParentClassOutput(),
                                 mce.getParentMethodOutput(),
                                 mce.getParentMethodDescriptor(),
                                 mce.getType(),
                                 mce.getDescriptor(),
                                 usesMappingOnlyForOutputKeys()), Function.identity()
                             ));

        this.inputCache = getCacheFromDatabase(true).parallelStream()
                            .collect(Collectors.toMap(
                              mce -> new MappingKey(mce.getInput(),
                                mce.getMappableType(),
                                mce.getParentClassOutput(),
                                mce.getParentMethodOutput(),
                                mce.getParentMethodDescriptor(),
                                mce.getType(),
                                mce.getDescriptor(),
                                usesMappingOnlyForInputKeys()), Function.identity()
                            ));

        LOGGER.info("Remapping all method descriptors, parameter and field types to mappable ids for: " + mappingTypeIds);
        this.outputCache = this.outputCache.values()
                             .stream()
                             .map(mce -> {
                                 if (mce.getMappableType() == MappableTypeDMO.CLASS)
                                 {
                                     return mce;
                                 }

                                 if (mce.getMappableType() == MappableTypeDMO.METHOD)
                                 {
                                     mce.setDescriptor(this.remapDescriptor(mce.getDescriptor()));
                                 }
                                 if (mce.getMappableType() == MappableTypeDMO.PARAMETER)
                                 {
                                     mce.setParentMethodDescriptor(this.remapDescriptor(mce.getParentMethodDescriptor()));
                                 }
                                 if (mce.getMappableType() == MappableTypeDMO.PARAMETER || mce.getMappableType() == MappableTypeDMO.FIELD)
                                 {
                                     mce.setType(this.remapType(mce.getType()));
                                 }

                                 return mce;
                             })
                             .collect(Collectors.toMap(
                               mce -> new MappingKey(mce.getOutput(),
                                 mce.getMappableType(),
                                 mce.getParentClassOutput(),
                                 mce.getParentMethodOutput(),
                                 mce.getParentMethodDescriptor(),
                                 mce.getType(),
                                 mce.getDescriptor(),
                                 usesMappingOnlyForOutputKeys()), Function.identity()
                             ));
        this.inputCache = this.inputCache.values()
                            .stream()
                            .map(mce -> {
                                if (mce.getMappableType() == MappableTypeDMO.CLASS)
                                {
                                    return mce;
                                }

                                if (mce.getMappableType() == MappableTypeDMO.METHOD)
                                {
                                    mce.setDescriptor(this.remapDescriptor(mce.getDescriptor()));
                                }
                                if (mce.getMappableType() == MappableTypeDMO.PARAMETER)
                                {
                                    mce.setParentMethodDescriptor(this.remapDescriptor(mce.getParentMethodDescriptor()));
                                }
                                if (mce.getMappableType() == MappableTypeDMO.PARAMETER || mce.getMappableType() == MappableTypeDMO.FIELD)
                                {
                                    mce.setType(this.remapType(mce.getType()));
                                }

                                return mce;
                            })
                            .collect(Collectors.toMap(
                              mce -> new MappingKey(mce.getInput(),
                                mce.getMappableType(),
                                mce.getParentClassOutput(),
                                mce.getParentMethodOutput(),
                                mce.getParentMethodDescriptor(),
                                mce.getType(),
                                mce.getDescriptor(),
                                usesMappingOnlyForInputKeys()), Function.identity()
                            ));

        LOGGER.info("Grabbing global mappables for: " + mappingTypeIds);
        this.mappableCache = this.databaseClient.select()
                               .from(MappableDMO.class)
                               .fetch()
                               .all()
                               .collectMap(MappableDMO::getId)
                               .block();

        LOGGER.info("Grabbing release information for: " + mappingTypeIds);
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

        Assert.notNull(releaseDMOS, "Releases are null!");

        this.releaseIdCache = releaseDMOS
                                .stream()
                                .collect(Collectors.toMap(ReleaseDMO::getId, Function.identity()));

        this.releaseNameCache = this.releaseIdCache.values().stream()
                                  .collect(Collectors.toMap(r -> Tuples.of(r.getMappingTypeId(), r.getName()), Function.identity()));

        LOGGER.info("Processing caches into smaller maps for faster lookups for: " + mappingTypeIds);
        this.versionedMappableIdClassCache = this.outputCache.values().parallelStream()
                                               .filter(mce -> mce.getMappableType() == MappableTypeDMO.CLASS)
                                               .collect(Collectors.groupingBy(MappingCacheEntry::getVersionedMappableId))
                                               .values().parallelStream()
                                               .map(entries -> {
                                                   if (entries.size() > 1)
                                                       System.out.println("Found it");

                                                   return entries.stream().min(((Comparator<MappingCacheEntry>) (left, right) -> {
                                                       final MinecraftVersion l = MinecraftVersion.from(left.getGameVersionName());
                                                       final MinecraftVersion r = MinecraftVersion.from(right.getGameVersionName());

                                                       return -1 * l.compareTo(r);
                                                   }).thenComparing((left, right) -> {
                                                       final ReleaseDMO l = releaseIdCache.get(left.getReleaseId());
                                                       final ReleaseDMO r = releaseIdCache.get(right.getReleaseId());

                                                       return -1 * l.getCreatedOn().compareTo(r.getCreatedOn());
                                                   })).get();
                                               })
                                               .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdMethodCache = this.outputCache.values().parallelStream()
                                                .filter(mce -> mce.getMappableType() == MappableTypeDMO.METHOD)
                                                .collect(Collectors.groupingBy(MappingCacheEntry::getVersionedMappableId))
                                                .values().parallelStream()
                                                .map(entries -> {
                                                    if (entries.size() > 1)
                                                        System.out.println("Found it");

                                                    return entries.stream().min(((Comparator<MappingCacheEntry>) (left, right) -> {
                                                        final MinecraftVersion l = MinecraftVersion.from(left.getGameVersionName());
                                                        final MinecraftVersion r = MinecraftVersion.from(right.getGameVersionName());

                                                        return -1 * l.compareTo(r);
                                                    }).thenComparing((left, right) -> {
                                                        final ReleaseDMO l = releaseIdCache.get(left.getReleaseId());
                                                        final ReleaseDMO r = releaseIdCache.get(right.getReleaseId());

                                                        return -1 * l.getCreatedOn().compareTo(r.getCreatedOn());
                                                    })).get();
                                                })
                                                .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdFieldCache = this.outputCache.values().parallelStream()
                                               .filter(mce -> mce.getMappableType() == MappableTypeDMO.FIELD)
                                               .collect(Collectors.groupingBy(MappingCacheEntry::getVersionedMappableId))
                                               .values().parallelStream()
                                               .map(entries -> {
                                                   if (entries.size() > 1)
                                                       System.out.println("Found it");

                                                   return entries.stream().min(((Comparator<MappingCacheEntry>) (left, right) -> {
                                                       final MinecraftVersion l = MinecraftVersion.from(left.getGameVersionName());
                                                       final MinecraftVersion r = MinecraftVersion.from(right.getGameVersionName());

                                                       return -1 * l.compareTo(r);
                                                   }).thenComparing((left, right) -> {
                                                       final ReleaseDMO l = releaseIdCache.get(left.getReleaseId());
                                                       final ReleaseDMO r = releaseIdCache.get(right.getReleaseId());

                                                       return -1 * l.getCreatedOn().compareTo(r.getCreatedOn());
                                                   })).get();
                                               })
                                               .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.versionedMappableIdParameterCache = this.outputCache.values().parallelStream()
                                                   .filter(mce -> mce.getMappableType() == MappableTypeDMO.PARAMETER)
                                                   .collect(Collectors.groupingBy(MappingCacheEntry::getVersionedMappableId))
                                                   .values().parallelStream()
                                                   .map(entries -> {
                                                       if (entries.size() > 1)
                                                           System.out.println("Found it");

                                                       return entries.stream().min(((Comparator<MappingCacheEntry>) (left, right) -> {
                                                           final MinecraftVersion l = MinecraftVersion.from(left.getGameVersionName());
                                                           final MinecraftVersion r = MinecraftVersion.from(right.getGameVersionName());

                                                           return -1 * l.compareTo(r);
                                                       }).thenComparing((left, right) -> {
                                                           final ReleaseDMO l = releaseIdCache.get(left.getReleaseId());
                                                           final ReleaseDMO r = releaseIdCache.get(right.getReleaseId());

                                                           return -1 * l.getCreatedOn().compareTo(r.getCreatedOn());
                                                       })).get();
                                                   })
                                                   .collect(Collectors.toMap(MappingCacheEntry::getVersionedMappableId, Function.identity()));

        this.classMembers = this.outputCache.values().parallelStream()
                              .filter(mce -> mce.getParentClassOutput() != null)
                              .collect(Collectors.groupingBy(MappingCacheEntry::getParentClassOutput));

        LOGGER.warn("Rebuilding cache for: " + mappingTypeIds + " completed.");
    }

    private Set<MappingCacheEntry> getCacheFromDatabase(final boolean isInput)
    {
        final List<UUID> mappingTypeIds = getMappingTypeIds();
        final String mappingTypeFilterQueryComponent =
          mappingTypeIds.stream()
            .map(id -> "m.mapping_type_id = '" + id + "'")
            .reduce((left, right) -> left + " or " + right).orElseThrow();

        final String mappingColumnName = isInput ? "input" : "output";

        return databaseClient.execute(String.format(
          "SELECT  DISTINCT ON (m.%s, m.mapping_type_id, pcm.output, pmm.output, pmvm.descriptor, vm.descriptor)  m.input as input, m.output as output, mp.id as mappable_id, m.id as mapping_id, gv.created_on as game_version_created_on, vm.id as versioned_mappable_id, mp.type as mappable_type, pcm.output as parent_class_output, pmm.output as parent_method_output, pmvm.descriptor as parent_method_descriptor, gv.id as game_version_id, gv.name as game_version_name, vm.type as type, vm.descriptor as descriptor, vm.is_static as is_static, vm.index as index from mapping m\n"
            +
            "                        JOIN versioned_mappable vm on m.versioned_mappable_id = vm.id\n" +
            "                        JOIN game_version gv on vm.game_version_id = gv.id\n" +
            "                        JOIN mappable mp on vm.mappable_id = mp.id\n" +
            "                        LEFT OUTER JOIN mapping pcm ON vm.parent_class_id = pcm.versioned_mappable_id and pcm.mapping_type_id = m.mapping_type_id\n"
            +
            "                        LEFT OUTER JOIN mapping pmm ON vm.parent_method_id = pmm.versioned_mappable_id and pmm.mapping_type_id = m.mapping_type_id\n"
            +
            "                        LEFT OUTER JOIN versioned_mappable pmvm ON vm.parent_method_id = pmvm.id \n" +
            "                        WHERE (%s)\n" +
            "                        order by m.%s, m.mapping_type_id, pcm.output, pmm.output, pmvm.descriptor, vm.descriptor, gv.created_on desc, m.created_on desc;",
          mappingColumnName,
          mappingTypeFilterQueryComponent,
          mappingColumnName))
                 .as(MappingCacheEntry.class)
                 .fetch()
                 .all()
                 .collect(Collectors.toSet())
                 .block();
    }

    protected abstract List<UUID> getMappingTypeIds();

    public void destroyCache()
    {
        this.outputCache = Collections.emptyMap();
        this.inputCache = Collections.emptyMap();
        this.mappableCache = Collections.emptyMap();
        this.versionedMappableIdClassCache = Collections.emptyMap();
        this.versionedMappableIdMethodCache = Collections.emptyMap();
        this.versionedMappableIdFieldCache = Collections.emptyMap();
        this.versionedMappableIdParameterCache = Collections.emptyMap();
        this.classMembers = Collections.emptyMap();
        this.gameVersionIdCache = Collections.emptyMap();
        this.gameVersionNameCache = Collections.emptyMap();
        this.releaseIdCache = Collections.emptyMap();
        this.releaseNameCache = Collections.emptyMap();
        this.superTypeCache = Maps.newHashMap();
        this.overridesMethodsCache = Maps.newHashMap();
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

    public MappingCacheEntry getClassViaOutput(String mapping)
    {
        String parent = null;
        if (mapping.contains("$"))
        {
            parent = mapping.substring(0, mapping.lastIndexOf("$"));
        }


        final MappingKey id = new MappingKey(mapping,
          MappableTypeDMO.CLASS,
          parent,
          null,
          null,
          null,
          null,
          usesMappingOnlyForOutputKeys());
        final MappingCacheEntry clz = this.outputCache.get(id);
        if (clz == null)
        {
            return null;
        }

        return clz;
    }

    public MappingCacheEntry getMethodViaOutput(String mapping, String parentClass, String descriptor)
    {
        if (descriptor.length() != 0)
            descriptor = this.remapDescriptor(descriptor);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.METHOD,
          parentClass,
          null,
          null,
          null,
          descriptor,
          usesMappingOnlyForOutputKeys()
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getFieldViaOutput(String mapping, String parentClass, String type)
    {
        if (type.length() != 0)
        type = this.remapType(type);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.FIELD,
          parentClass,
          null,
          null,
          type,
          null,
          usesMappingOnlyForOutputKeys()
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getParameterViaOutput(String mapping, String parentClass, String parentMethod, String parentMethodDescriptor, String type)
    {
        if (parentMethodDescriptor.length() != 0)
            parentMethodDescriptor = this.remapDescriptor(parentMethodDescriptor);

        if (type.length() != 1)
            type = this.remapType(type);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.PARAMETER,
          parentClass,
          parentMethod,
          parentMethodDescriptor,
          type,
          null,
          usesMappingOnlyForOutputKeys()
        );
        return this.outputCache.get(id);
    }

    public MappingCacheEntry getClassViaInput(String mapping)
    {
        String parent = null;
        if (mapping.contains("$"))
        {
            parent = mapping.substring(0, mapping.lastIndexOf("$"));

            final MappingCacheEntry parentMce = getClassViaInput(parent);
            //If i can not find my outer class. I am never going to be able to find my inner class in the mappings
            //So lets just quit while we are ahead.
            if (parentMce == null)
            {
                return null;
            }

            parent = parentMce.getOutput();
        }

        final MappingKey id = new MappingKey(mapping,
          MappableTypeDMO.CLASS,
          parent,
          null,
          null,
          null,
          null,
          usesMappingOnlyForInputKeys());
        final MappingCacheEntry clz = this.inputCache.get(id);
        if (clz == null)
        {
            return null;
        }

        return clz;
    }

    public MappingCacheEntry getMethodViaInput(String mapping, String parentClass, String descriptor)
    {
        if (descriptor.length() != 0)
            descriptor = this.remapDescriptor(descriptor);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.METHOD,
          parentClass,
          null,
          null,
          null,
          descriptor,
          usesMappingOnlyForInputKeys()
        );

        if (this.inputCache.get(id) == null)
        {
            return null;
        }

        return this.inputCache.get(id);
    }

    public MappingCacheEntry getFieldViaInput(String mapping, String parentClass, String type)
    {
        if(type.length() != 0)
            type = this.remapType(type);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.FIELD,
          parentClass,
          null,
          null,
          type,
          null,
          usesMappingOnlyForInputKeys()
        );

        if (this.inputCache.get(id) == null)
        {
            return null;
        }

        return this.inputCache.get(id);
    }

    public MappingCacheEntry getParameterViaInput(String mapping, String parentClass, String parentMethod, String parentMethodDescriptor, String type)
    {
        if (parentMethodDescriptor.length() != 0)
            parentMethodDescriptor = this.remapDescriptor(parentMethodDescriptor);

        if (type.length() != 0)
            type = this.remapType(type);

        final MappingKey id = new MappingKey(
          mapping,
          MappableTypeDMO.PARAMETER,
          parentClass,
          parentMethod,
          parentMethodDescriptor,
          type,
          null,
          usesMappingOnlyForInputKeys()
        );
        return this.inputCache.get(id);
    }

    public Map<MappingKey, MappingCacheEntry> getAllInClassFromInput(final String classInput)
    {
        return this.inputCache.keySet().stream().filter(mappingKey -> mappingKey.getParentClassMapping() != null && mappingKey.getParentClassMapping().equals(classInput))
                 .collect(Collectors.toMap(
                   Function.identity(),
                   key -> this.inputCache.get(key)
                 ));
    }

    public void registerNewGameVersion(final GameVersionDMO gameVersion)
    {
        this.gameVersionIdCache.put(gameVersion.getId(), gameVersion);
        this.gameVersionNameCache.put(gameVersion.getName(), gameVersion);
    }

    public void registerNewRelease(final ReleaseDMO release)
    {
        this.releaseIdCache.put(release.getId(), release);
        this.releaseNameCache.put(Tuples.of(release.getMappingTypeId(), release.getName()), release);
    }

    public void registerNewClass(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping, final ReleaseComponentDMO releaseComponentDMO)
    {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
          mapping.getInput(),
          mapping.getOutput(),
          mappable.getId(),
          versionedMappable.getId(),
          mapping.getId(),
          MappableTypeDMO.CLASS,
          versionedMappable.getParentClassId() != null && this.versionedMappableIdClassCache.containsKey(versionedMappable.getParentClassId())
            ? this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput()
            : null,
          null,
          null,
          versionedMappable.getGameVersionId(),
          gameVersionIdCache.get(versionedMappable.getGameVersionId()).getName(),
          releaseComponentDMO.getReleaseId(),
          null, null, -1, versionedMappable.isStatic());

        final MappingKey inputMappingKey = new MappingKey(
          newEntry.getInput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForInputKeys()
        );

        final MappingKey outputMappingKey = new MappingKey(
          newEntry.getOutput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForOutputKeys()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdClassCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewMethod(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping, final ReleaseComponentDMO releaseComponentDMO)
    {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
          mapping.getInput(),
          mapping.getOutput(),
          mappable.getId(),
          versionedMappable.getId(),
          mapping.getId(),
          MappableTypeDMO.METHOD,
          this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
          null,
          null,
          versionedMappable.getGameVersionId(),
          getGameVersion(versionedMappable.getGameVersionId()).getName(),
          releaseComponentDMO.getReleaseId(),
          null,
          versionedMappable.getDescriptor(), -1, versionedMappable.isStatic());

        newEntry.setDescriptor(this.remapDescriptor(newEntry.getDescriptor()));

        final MappingKey inputMappingKey = new MappingKey(
          newEntry.getInput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForInputKeys()
        );

        final MappingKey outputMappingKey = new MappingKey(
          newEntry.getOutput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForOutputKeys()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdMethodCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewField(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping, final ReleaseComponentDMO releaseComponentDMO)
    {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
          mapping.getInput(),
          mapping.getOutput(),
          mappable.getId(),
          versionedMappable.getId(),
          mapping.getId(),
          MappableTypeDMO.FIELD,
          this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
          null,
          null,
          versionedMappable.getGameVersionId(),
          getGameVersion(versionedMappable.getGameVersionId()).getName(),
          releaseComponentDMO.getReleaseId(),
          versionedMappable.getType(), null, -1, versionedMappable.isStatic());

        newEntry.setType(this.remapType(newEntry.getType()));

        final MappingKey inputMappingKey = new MappingKey(
          newEntry.getInput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForInputKeys()
        );

        final MappingKey outputMappingKey = new MappingKey(
          newEntry.getOutput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForOutputKeys()
        );

        this.inputCache.put(inputMappingKey, newEntry);
        this.outputCache.put(outputMappingKey, newEntry);
        this.mappableCache.put(mappable.getId(), mappable);
        this.versionedMappableIdFieldCache.put(versionedMappable.getId(), newEntry);
    }

    public void registerNewParameter(final MappableDMO mappable, final VersionedMappableDMO versionedMappable, final MappingDMO mapping, final ReleaseComponentDMO releaseComponentDMO)
    {
        final MappingCacheEntry newEntry = new MappingCacheEntry(
          mapping.getInput(),
          mapping.getOutput(),
          mappable.getId(),
          versionedMappable.getId(),
          mapping.getId(),
          MappableTypeDMO.PARAMETER,
          this.versionedMappableIdClassCache.get(versionedMappable.getParentClassId()).getOutput(),
          this.versionedMappableIdMethodCache.get(versionedMappable.getParentMethodId()).getOutput(),
          this.versionedMappableIdMethodCache.get(versionedMappable.getParentMethodId()).getDescriptor(),
          versionedMappable.getGameVersionId(),
          getGameVersion(versionedMappable.getGameVersionId()).getName(),
          releaseComponentDMO.getReleaseId(),
          versionedMappable.getType(), null, versionedMappable.getIndex(), versionedMappable.isStatic());

        newEntry.setParentMethodDescriptor(this.remapDescriptor(newEntry.getParentMethodDescriptor()));
        newEntry.setType(this.remapType(newEntry.getType()));

        final MappingKey inputMappingKey = new MappingKey(
          newEntry.getInput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForInputKeys()
        );

        final MappingKey outputMappingKey = new MappingKey(
          newEntry.getOutput(),
          newEntry.getMappableType(),
          newEntry.getParentClassOutput(),
          newEntry.getParentMethodOutput(),
          newEntry.getParentMethodDescriptor(),
          newEntry.getType(),
          newEntry.getDescriptor(),
          usesMappingOnlyForOutputKeys()
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
                   Mono.just(new MappingTypeDMO(UUID.randomUUID(),
                     Constants.SYSTEM_ID,
                     Timestamp.from(Instant.now()),
                     mappingTypeName,
                     isVisible,
                     isEditable,
                     stateIn,
                     stateOut))
                     .flatMap(mappingType -> databaseClient.insert()
                                               .into(MappingTypeDMO.class)
                                               .using(mappingType)
                                               .fetch()
                                               .first()
                                               .map(r -> mappingType))
                 )
                 .map(MappingTypeDMO::getId).blockOptional().orElse(null);
    }

    public MappingCacheEntry getClassCacheEntry(final UUID id)
    {
        return this.versionedMappableIdClassCache.get(id);
    }

    public MappingCacheEntry getMethodCacheEntry(final UUID id)
    {
        return this.versionedMappableIdMethodCache.get(id);
    }

    public MappingCacheEntry getFieldCacheEntry(final UUID id)
    {
        return this.versionedMappableIdFieldCache.get(id);
    }

    public MappingCacheEntry getParameterCacheEntry(final UUID id)
    {
        return this.versionedMappableIdParameterCache.get(id);
    }

    public Collection<MappingCacheEntry> getAllClasses() {
        return this.versionedMappableIdClassCache.values();
    }

    public Collection<MappingCacheEntry> getAllMethods() {
        return this.versionedMappableIdMethodCache.values();
    }

    public Collection<MappingCacheEntry> getAllFields() {
        return this.versionedMappableIdFieldCache.values();
    }

    public Collection<MappingCacheEntry> getAllParameters() {
        return this.versionedMappableIdParameterCache.values();
    }

    public List<MappingCacheEntry> getConstructorsForClass(@NotNull final String classOutput)
    {
        return this.getAllMethodForClass(classOutput)
                 .stream()
                 .filter(mce -> (mce.getInput().equals("<init>") && mce.getOutput().equals("<init>"))
                                  || mce.getOutput().equals(NameUtils.getActualClassName(mce.getParentClassOutput())))
                 .collect(Collectors.toList());
    }

    public List<MappingCacheEntry> getAllMethodForClass(@NotNull final String classOutput)
    {
        return this.classMembers.getOrDefault(classOutput, Lists.newArrayList())
                 .stream()
                 .filter(mce -> mce.getMappableType() == MappableTypeDMO.METHOD)
                 .collect(Collectors.toList());
    }

    public List<MappingCacheEntry> getAllFieldsForClass(@NotNull final String classOutput)
    {
        return this.classMembers.getOrDefault(classOutput, Lists.newArrayList())
                 .stream()
                 .filter(mce -> mce.getMappableType() == MappableTypeDMO.FIELD)
                 .collect(Collectors.toList());
    }

    public List<MappingCacheEntry> getAllParameters(@NotNull final String classOutput)
    {
        return this.classMembers.getOrDefault(classOutput, Lists.newArrayList())
                 .stream()
                 .filter(mce -> mce.getMappableType() == MappableTypeDMO.PARAMETER)
                 .collect(Collectors.toList());
    }

    public void setRemappingManager(final AbstractMappingCacheManager remappingManager)
    {
        this.remappingManager = remappingManager;
    }

    public String remapDescriptor(final String descriptor)
    {
        final MethodDesc methodDesc = new MethodDesc(descriptor);
        final MethodDesc remappedMethodDesc = methodDesc.remap(
          type -> Optional.ofNullable(this.remappingManager.getClassViaInput(type))
                    .map(MappingCacheEntry::getMappableId)
                    .map(Objects::toString)
        );
        return remappedMethodDesc.toString();
    }

    public String remapType(final String type)
    {
        return new RemappableType(type).remap(s -> Optional.ofNullable(this.remappingManager.getClassViaInput(s))
                                                     .map(MappingCacheEntry::getMappableId)
                                                     .map(Objects::toString)).getType();
    }

    public List<UUID> getSuperTypes(String gameVersionName, UUID classVersionedMappableId)
    {
        loadSuperTypes(gameVersionName);

        return this.superTypeCache.getOrDefault(gameVersionName, Maps.newHashMap()).getOrDefault(classVersionedMappableId, Lists.newArrayList());
    }

    private void loadSuperTypes(final String gameVersionName)
    {
        if (this.superTypeCache.containsKey(gameVersionName))
        {
            return;
        }

        if (!this.gameVersionNameCache.containsKey(gameVersionName))
        {
            return;
        }

        LOGGER.warn("Loading super type information in gameversion: " + gameVersionName + " for: " + getMappingTypeIds());

        final GameVersionDMO gv = this.gameVersionNameCache.get(gameVersionName);

        final Map<UUID, List<UUID>> overrideData = this.databaseClient
                                                     .execute(
                                                       "Select id.sub_type_versioned_mappable_id as subClass, id.super_type_versioned_mappable_id as superClass from mmms.public.inheritance_data id "
                                                         + "JOIN mmms.public.versioned_mappable vm on vm.id = id.sub_type_versioned_mappable_id "
                                                         + "JOIN mmms.public.mappable mp on vm.mappable_id = mp.id "
                                                         + "WHERE mp.type = 'CLASS' and vm.game_version_id = '" + gv.getId() + "'"
                                                     ).as(OverrideEntry.class)
                                                     .fetch()
                                                     .all()
                                                     .collect(Collectors.groupingBy(OverrideEntry::getSubClass))
                                                     .map(rawData -> rawData.keySet().stream()
                                                                       .collect(Collectors.toMap(Function.identity(),
                                                                         id -> rawData.get(id).stream().map(OverrideEntry::getSuperClass).collect(Collectors.toList()))))
                                                     .blockOptional().orElse(Maps.newHashMap());

        this.superTypeCache.put(gameVersionName, overrideData);
    }

    public List<UUID> getOverrides(String gameVersionName, UUID classMethodVersionedId)
    {
        loadOverrides(gameVersionName);

        return this.overridesMethodsCache.getOrDefault(gameVersionName, Maps.newHashMap()).getOrDefault(classMethodVersionedId, Lists.newArrayList());
    }

    private void loadOverrides(final String gameVersionName)
    {
        if (this.overridesMethodsCache.containsKey(gameVersionName))
        {
            return;
        }

        if (!this.gameVersionNameCache.containsKey(gameVersionName))
        {
            return;
        }

        LOGGER.warn("Loading super type information in gameversion: " + gameVersionName + " for: " + getMappingTypeIds());

        final GameVersionDMO gv = this.gameVersionNameCache.get(gameVersionName);

        final Map<UUID, List<UUID>> overrideData = this.databaseClient
                                                     .execute(
                                                       "Select id.sub_type_versioned_mappable_id as sub_class, id.super_type_versioned_mappable_id as super_class from mmms.public.inheritance_data id "
                                                         + "JOIN mmms.public.versioned_mappable vm on vm.id = id.sub_type_versioned_mappable_id "
                                                         + "JOIN mmms.public.mappable mp on vm.mappable_id = mp.id "
                                                         + "WHERE mp.type = 'METHOD' and vm.game_version_id = '" + gv.getId() + "'"
                                                     ).as(OverrideEntry.class)
                                                     .fetch()
                                                     .all()
                                                     .collect(Collectors.groupingBy(OverrideEntry::getSubClass))
                                                     .map(rawData -> rawData.keySet().stream()
                                                                       .collect(Collectors.toMap(Function.identity(),
                                                                         id -> rawData.get(id).stream().map(OverrideEntry::getSuperClass).collect(Collectors.toList()))))
                                                     .blockOptional().orElse(Maps.newHashMap());

        this.overridesMethodsCache.put(gameVersionName, overrideData);
    }

    public boolean usesMappingOnlyForInputKeys()
    {
        return false;
    }

    public boolean usesMappingOnlyForOutputKeys() {
        return false;
    }

    private static final class OverrideEntry
    {
        private final UUID subClass;
        private final UUID superClass;

        public OverrideEntry(final UUID subClass, final UUID superClass)
        {
            this.subClass = subClass;
            this.superClass = superClass;
        }

        public UUID getSubClass()
        {
            return subClass;
        }

        public UUID getSuperClass()
        {
            return superClass;
        }
    }
}
