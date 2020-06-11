package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.AbstractMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;

import java.util.List;
import java.util.stream.Collectors;

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
                entry = cacheManager.getParameterViaOutput(externalMapping.getOutput(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodMapping(), externalMapping.getParentMethodDescriptor(), externalMapping.getType());
                break;
        }
        return entry;
    }

    public static MappableDMO getCachedMappableViaOutput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager)
    {
        final MappingCacheEntry entry = getOutputMappingCacheEntry(externalMapping, mappingCacheManager);
        if (entry == null)
            return null;

        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static boolean alreadyExistsOnInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager targetManager, final AbstractMappingCacheManager parentRemappingManager)
    {
        MappingCacheEntry entry = getInputMappingCacheEntry(externalMapping, targetManager, parentRemappingManager);
        return entry != null;
    }

    public static MappingCacheEntry getInputMappingCacheEntry(final ExternalMapping externalMapping, final AbstractMappingCacheManager targetCacheManager, final AbstractMappingCacheManager parentRemappingManager) {
        MappingCacheEntry entry = null;
        switch (externalMapping.getMappableType())
        {
            case CLASS:
                entry = targetCacheManager.getClassViaInput(externalMapping.getInput());
                break;
            case METHOD:
                final String parentMethodClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();
                final String targetMethodParentClassOutput = targetCacheManager.getClassViaInput(parentMethodClassInput).getOutput();

                entry = targetCacheManager.getMethodViaInput(externalMapping.getInput(), targetMethodParentClassOutput, externalMapping.getDescriptor());
                break;
            case FIELD:
                final String parentFieldClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();
                final String targetFieldParentClassOutput = targetCacheManager.getClassViaInput(parentFieldClassInput).getOutput();

                entry = targetCacheManager.getFieldViaInput(externalMapping.getInput(), targetFieldParentClassOutput, externalMapping.getType());
                break;
            case PARAMETER:
                final String parentParameterClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();
                final String targetParameterParentClassOutput = targetCacheManager.getClassViaInput(parentParameterClassInput).getOutput();

                if (parentRemappingManager.getMethodViaOutput(externalMapping.getParentMethodMapping(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodDescriptor()) == null)
                {
                    System.out.println("Hello");
                }


                final String parentParameterMethodInput = parentRemappingManager.getMethodViaOutput(externalMapping.getParentMethodMapping(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodDescriptor()).getInput();
                final String targetParameterParentMethodOutput = targetCacheManager.getMethodViaInput(parentParameterMethodInput, targetParameterParentClassOutput, externalMapping.getParentMethodDescriptor()).getOutput();


                entry = targetCacheManager.getParameterViaInput(externalMapping.getInput(), targetParameterParentClassOutput, targetParameterParentMethodOutput, externalMapping.getParentMethodDescriptor(), externalMapping.getType());
                break;
        }
        return entry;
    }

    public static MappingCacheEntry getInputMappingCacheEntry(final ExternalMapping externalMapping, final AbstractMappingCacheManager targetCacheManager) {
        MappingCacheEntry entry = null;
        switch (externalMapping.getMappableType())
        {
            case CLASS:
                entry = targetCacheManager.getClassViaInput(externalMapping.getInput());
                break;
            case METHOD:
                entry = targetCacheManager.getMethodViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getDescriptor());
                break;
            case FIELD:
                entry = targetCacheManager.getFieldViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getType());
                break;
            case PARAMETER:
                entry = targetCacheManager.getParameterViaInput(externalMapping.getInput(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodMapping(), externalMapping.getParentMethodDescriptor(), externalMapping.getType());
                break;
        }
        return entry;
    }

    public static boolean alreadyExistsOnOutputFromInput(final ExternalMapping ExternalMapping, final AbstractMappingCacheManager targetCacheManager, final AbstractMappingCacheManager parentRemappingManager)
    {
        MappingCacheEntry entry = getOutputMappingCacheEntryFromInput(ExternalMapping, targetCacheManager, parentRemappingManager);
        return entry != null;
    }

    public static MappingCacheEntry getOutputMappingCacheEntryFromInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager targetCacheManager, final AbstractMappingCacheManager parentRemappingManager) {
        MappingCacheEntry entry = null;
        switch (externalMapping.getMappableType())
        {
            case CLASS:
                entry = targetCacheManager.getClassViaOutput(externalMapping.getInput());
                break;
            case METHOD:
                final String parentMethodClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();

                entry = targetCacheManager.getMethodViaOutput(externalMapping.getInput(), parentMethodClassInput, externalMapping.getDescriptor());
                break;
            case FIELD:
                final String parentFieldClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();

                entry = targetCacheManager.getFieldViaOutput(externalMapping.getInput(), parentFieldClassInput, externalMapping.getType());
                break;
            case PARAMETER:
                final String parentParameterClassInput = parentRemappingManager.getClassViaOutput(externalMapping.getParentClassMapping()).getInput();
                final String parentParameterMethodInput = parentRemappingManager.getMethodViaOutput(externalMapping.getParentMethodMapping(), externalMapping.getParentClassMapping(), externalMapping.getParentMethodDescriptor()).getInput();

                entry = targetCacheManager.getParameterViaOutput(externalMapping.getInput(), parentParameterClassInput, parentParameterMethodInput, externalMapping.getParentMethodDescriptor(), externalMapping.getType());
                break;
        }
        return entry;
    }


    public static MappableDMO getCachedMappableViaInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager, final AbstractMappingCacheManager parentRemappingManager)
    {
        final MappingCacheEntry entry = getInputMappingCacheEntry(externalMapping, mappingCacheManager, parentRemappingManager);
        if (entry == null)
            return null;
        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static MappableDMO getCachedMappableViaInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager)
    {
        final MappingCacheEntry entry = getInputMappingCacheEntry(externalMapping, mappingCacheManager);
        if (entry == null)
            return null;
        return mappingCacheManager.getMappable(entry.getMappableId());
    }

    public static MappableDMO getCachedMappableViaOutputFromInput(final ExternalMapping externalMapping, final AbstractMappingCacheManager mappingCacheManager, final AbstractMappingCacheManager parentRemappingManager)
    {
        final MappingCacheEntry entry = getOutputMappingCacheEntryFromInput(externalMapping, mappingCacheManager, parentRemappingManager);
        if (entry == null)
            return null;

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

    public static String remapDescriptorFromOutputToInput(
            final String desc,
            final AbstractMappingCacheManager mappingCacheManager
    )
    {
        final MethodDesc methodDesc = new MethodDesc(desc);
        final List<String> remappedArgs = methodDesc.getArgs().stream().map(arg -> remapToInput(arg, mappingCacheManager)).collect(Collectors.toList());
        final String remappedReturnType = remapToInput(methodDesc.getReturnType(), mappingCacheManager);

        return "(" + String.join("", remappedArgs) + ")" + remappedReturnType;
    }

    private static String remapToInput(
            final String clz,
            final AbstractMappingCacheManager mappingCacheManager
    )
    {
        if (clz.length() == 1)
            return clz; //Early bail out for primitives.

        if (clz.startsWith("["))
            return "[" + remapToInput(clz.substring(1), mappingCacheManager); //Handles arrays.

        if (clz.startsWith("L"))
            return "L" + remapToInput(clz.substring(1), mappingCacheManager); // Handles class prefixes.

        final MappingCacheEntry entry = mappingCacheManager.getClassViaOutput(clz.replace(";", ""));
        if (entry == null)
            return clz;

        return entry.getInput() + ";";
    }
}
