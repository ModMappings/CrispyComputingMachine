package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.AbstractSimpleBasedCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;

import java.util.UUID;

public class OutputCacheUtils {

    private OutputCacheUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: OutputCacheUtils. This is a utility class");
    }

    public static UUID getMappableId(final AbstractSimpleBasedCacheManager cacheManager, ExternalVanillaMapping externalVanillaMapping)
    {
        switch (externalVanillaMapping.getMappableType())
        {
            case CLASS:
                return cacheManager.getMappableIdForClassFromOutput(externalVanillaMapping.getOutput());
            case METHOD:
                return cacheManager.getMappableIdForMethod(externalVanillaMapping.getOutput(), externalVanillaMapping.getDescriptor(), externalVanillaMapping.getParentClassMapping());
            case FIELD:
                return cacheManager.getMappableIdForField(externalVanillaMapping.getOutput(), externalVanillaMapping.getType(), externalVanillaMapping.getParentClassMapping());
            case PARAMETER:
                return cacheManager.getMappableIdForParameter(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getParentMethodMapping(), externalVanillaMapping.getParentMethodDescriptor(), externalVanillaMapping.getType());
        }

        return null;
    }

    public static UUID getRemoteMappableId(final AbstractSimpleBasedCacheManager cacheManager, ExternalVanillaMapping externalVanillaMapping)
    {
        switch (externalVanillaMapping.getMappableType())
        {
            case CLASS:
                return cacheManager.getRemoteMappableIdForClassFromOutput(externalVanillaMapping.getOutput());
            case METHOD:
                return cacheManager.getRemoteMappableIdForMethod(externalVanillaMapping.getOutput(), externalVanillaMapping.getDescriptor(), externalVanillaMapping.getParentClassMapping());
            case FIELD:
                return cacheManager.getRemoteMappableIdForField(externalVanillaMapping.getOutput(), externalVanillaMapping.getType(), externalVanillaMapping.getParentClassMapping());
            case PARAMETER:
                return cacheManager.getRemoteMappableIdForParameter(externalVanillaMapping.getOutput(), externalVanillaMapping.getParentClassMapping(), externalVanillaMapping.getParentMethodMapping(), externalVanillaMapping.getParentMethodDescriptor(), externalVanillaMapping.getType());
        }

        return null;
    }

    public static void add(final AbstractSimpleBasedCacheManager cacheManager, final VersionedMappableDMO versionedMappable, final MappingDMO mapping)
    {
        switch (mapping.getMappableType())
        {
            case CLASS:
                cacheManager.addClass(mapping.getOutput(), mapping.getMappableId(), mapping.getVersionedMappableId());
                break;
            case METHOD:
                cacheManager.addMethod(mapping.getOutput(), versionedMappable.getDescriptor(), versionedMappable.getParentClassId(), mapping.getMappableId(), mapping.getVersionedMappableId());
                break;
            case FIELD:
                cacheManager.addField(mapping.getOutput(), versionedMappable.getType(), versionedMappable.getParentClassId(), mapping.getMappableId(), mapping.getVersionedMappableId());
                break;
            case PARAMETER:
                cacheManager.addParameter(mapping.getOutput(), versionedMappable.getParentClassId(), versionedMappable.getParentMethodId(), versionedMappable.getType(), mapping.getMappableId(), mapping.getVersionedMappableId());
                break;
        }
    }
}
