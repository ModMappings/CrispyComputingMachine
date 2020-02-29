package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.MappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;

public final class CacheUtils {

    private CacheUtils() {
        throw new IllegalStateException("Tried to initialize: CacheUtils but this is a Utility class.");
    }

    public static boolean vanillaAlreadyExists(final ExternalVanillaMapping externalVanillaMapping, final MappingCacheManager cacheManager)
    {
        MappingCacheEntry entry = null;
        switch (externalVanillaMapping.getMappableType())
        {
            case CLASS:
                entry = cacheManager.getClass(externalVanillaMapping.getOutput());
                break;
            case METHOD:
                entry = cacheManager.getMethod(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping());
                break;
            case FIELD:
                entry = cacheManager.getField(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping());
                break;
            case PARAMETER:
                entry = cacheManager.getParameter(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getParentMethodMapping());
                break;
        }

        return entry != null;
    }
}
