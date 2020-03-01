package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.MappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;

public final class CacheUtils {

    private CacheUtils() {
        throw new IllegalStateException("Tried to initialize: CacheUtils but this is a Utility class.");
    }

    public static boolean vanillaAlreadyExists(final ExternalVanillaMapping externalVanillaMapping, final MappingCacheManager cacheManager)
    {
        MappingCacheEntry entry = getMappingCacheEntry(externalVanillaMapping, cacheManager);
        return entry != null;
    }

    public static MappingCacheEntry getMappingCacheEntry(final ExternalVanillaMapping externalVanillaMapping, final MappingCacheManager cacheManager) {
        MappingCacheEntry entry = null;
        switch (externalVanillaMapping.getMappableType())
        {
            case CLASS:
                entry = cacheManager.getClass(externalVanillaMapping.getOutput());
                break;
            case METHOD:
                entry = cacheManager.getMethod(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getDescriptor());
                break;
            case FIELD:
                entry = cacheManager.getField(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getType());
                break;
            case PARAMETER:
                entry = cacheManager.getParameter(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getParentMethodMapping(), externalVanillaMapping.getType());
                break;
        }
        return entry;
    }

    public static MappableDMO getCachedMappable(final ExternalVanillaMapping externalVanillaMapping, final MappingCacheManager mappingCacheManager)
    {
        final MappingCacheEntry entry = getMappingCacheEntry(externalVanillaMapping, mappingCacheManager);
        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static void registerNewEntry(
            final MappableDMO mappable,
            final VersionedMappableDMO versionedMappable,
            final MappingDMO mapping,
            final MappingCacheManager mappingCacheManager
    )
    {
        switch (mappable.getType())
        {
            case CLASS:
                mappingCacheManager.registerNewClass(mappable, versionedMappable, mapping);
                break;
            case METHOD:
                mappingCacheManager.registerNewMethod(mappable, versionedMappable, mapping);
                break;
            case FIELD:
                mappingCacheManager.registerNewField(mappable, versionedMappable, mapping);
                break;
            case PARAMETER:
                mappingCacheManager.registerNewParameter(mappable, versionedMappable, mapping);
                break;
        }
    }
}
