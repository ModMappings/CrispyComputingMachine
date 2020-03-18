package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;

public final class CacheUtils {

    private CacheUtils() {
        throw new IllegalStateException("Tried to initialize: CacheUtils but this is a Utility class.");
    }

    public static boolean alreadyExistsOnOutput(final ExternalMapping ExternalMapping, final AbstractMappingCacheManager cacheManager)
    {
        MappingCacheEntry entry = getOutputMappingCacheEntry(ExternalMapping, cacheManager);
        return entry != null;
    }

    public static MappingCacheEntry getOutputMappingCacheEntry(final ExternalMapping externalMapping, final AbstractMappingCacheManager cacheManager) {
        MappingCacheEntry entry = null;
        switch (externalMapping.getMappableType())
        {
            case CLASS:
                entry = cacheManager.getClassViaOutput(externalMapping.getOutput());
                break;
            case METHOD:
                entry = cacheManager.getMethodViaOutput(externalMapping.getOutput(), externalMapping.getParentClassMapping(), externalMapping.getDescriptor());
                break;
            case FIELD:
                entry = cacheManager.getFieldViaOutput(externalMapping.getOutput(), externalMapping.getParentClassMapping(), externalMapping.getType());
                break;
            case PARAMETER:
                entry = cacheManager.getParameterViaOutput(externalMapping.getOutput(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodMapping(), externalMapping.getType());
                break;
        }
        return entry;
    }

    public static MappableDMO getCachedMappableViaOutput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager)
    {
        final MappingCacheEntry entry = getOutputMappingCacheEntry(externalMapping, mappingCacheManager);
        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static boolean alreadyExistsOnInput(final ExternalMapping ExternalMapping, final AbstractMappingCacheManager cacheManager)
    {
        MappingCacheEntry entry = getInputMappingCacheEntry(ExternalMapping, cacheManager);
        return entry != null;
    }

    public static MappingCacheEntry getInputMappingCacheEntry(final ExternalMapping externalMapping, final AbstractMappingCacheManager cacheManager) {
        MappingCacheEntry entry = null;
        switch (externalMapping.getMappableType())
        {
            case CLASS:
                entry = cacheManager.getClassViaInput(externalMapping.getInput());
                break;
            case METHOD:
                entry = cacheManager.getMethodViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getDescriptor());
                break;
            case FIELD:
                entry = cacheManager.getFieldViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getType());
                break;
            case PARAMETER:
                entry = cacheManager.getParameterViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodMapping(), externalMapping.getType());
                break;
        }
        return entry;
    }

    public static MappableDMO getCachedMappableViaInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager)
    {
        final MappingCacheEntry entry = getInputMappingCacheEntry(externalMapping, mappingCacheManager);
        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static void registerNewEntry(
            final MappableDMO mappable,
            final VersionedMappableDMO versionedMappable,
            final MappingDMO mapping,
            final AbstractMappingCacheManager mappingCacheManager
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
