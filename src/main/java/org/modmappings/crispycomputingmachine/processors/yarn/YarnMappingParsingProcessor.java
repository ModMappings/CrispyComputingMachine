package org.modmappings.crispycomputingmachine.processors.yarn;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.cache.IntermediaryMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParsingPostProcessor;
import org.modmappings.crispycomputingmachine.utils.CacheUtils;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodDesc;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Component
public class YarnMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    public YarnMappingParsingProcessor(
            final VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager,
            final IntermediaryMappingCacheManager intermediaryCache
            ) {
        super(
                (releaseName) -> Lists.asList(Paths.get(releaseName, Constants.YARN_WORKING_DIR, "mappings", "mappings.tiny"), new Path[0]),
                (line, releaseName) -> {
                    if (!line.startsWith("c"))
                        return null;

                    final String workingLine = line.trim();

                    final String[] components = workingLine.split("\t");
                    if (components.length != 3)
                        return null;

                    String parentClassOut = null;
                    if (components[2].contains("$"))
                        parentClassOut = components[2].substring(0, components[2].indexOf("$"));

                    return new ExternalMapping(
                            components[1],
                            components[2],
                            ExternalMappableType.CLASS,
                            releaseName.split("\\+")[0],
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false);
                },
                (parentClass, line, releaseName) -> {
                    final String workingLine = line.trim();

                    if (!workingLine.startsWith("m"))
                        return null;

                    final String[] components = workingLine.split("\t");
                    if (components.length != 4)
                        return null;

                    final String desc = CacheUtils.remapDescriptorFromOutputToInput(components[1], intermediaryCache);
                    final String parentClassOut = parentClass.getOutput();

                    return new ExternalMapping(
                            components[2],
                            components[3],
                            ExternalMappableType.METHOD,
                            releaseName.split("\\+")[0],
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            null,
                            desc,
                            null,
                            null,
                            false);
                },
                (parentClass, line, releaseName) -> {
                    final String workingLine = line.trim();

                    if (!workingLine.startsWith("f"))
                        return null;

                    final String[] components = workingLine.split("\t");
                    if (components.length != 4)
                        return null;

                    final String parentClassOut = parentClass.getOutput();

                    return new ExternalMapping(
                            components[2],
                            components[3],
                            ExternalMappableType.FIELD,
                            releaseName.split("\\+")[0],
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            "*", //The type is irrelevant here. Fields are unique with in the class. Unless somebody is doing bytecode magic.
                            null,
                            null,
                            null,
                            false);
                },
                (parentClass, parentMethod, line, releaseName) -> {
                    final String workingLine = line.trim();
                    if (!workingLine.startsWith("p"))
                        return null;

                    final String[] components = workingLine.split("\t");
                    if (components.length != 4)
                        return null;


                    final int parameterIndex = Integer.parseInt(components[1]);

                    MappingCacheEntry entry = intermediaryCache.getMethodViaOutput(
                        parentMethod.getInput(),
                        parentClass.getInput(),
                        parentMethod.getDescriptor()
                    );

                    if (entry == null && parentMethod.getInput().equals(parentMethod.getOutput()))
                    {
                        //We are probably in an external method. Or constructor. Lets see if vanilla has information which we can use.
                        //First remap the class to vanilla mappings.
                        final MappingCacheEntry intermediaryClassMapping = intermediaryCache.getClassViaOutput(parentClass.getInput());
                        if (intermediaryClassMapping != null)
                        {
                            final MappingCacheEntry vanillaClassMapping = vanillaAndExternalMappingCacheManager.getClassViaInput(intermediaryClassMapping.getInput());

                            if (vanillaClassMapping != null)
                            {
                                entry = vanillaAndExternalMappingCacheManager.getMethodViaInput(
                                        parentMethod.getInput(),
                                        vanillaClassMapping.getOutput(),
                                        parentMethod.getDescriptor()
                                );
                            }
                        }
                    }

                    if (entry == null && parameterIndex == 0) {
                        LOGGER.warn("Could not find a known already imported method that belongs to: " + parentMethod);
                        LOGGER.warn("Falling back to index based detection and marking the method as static, since index 0 is present");
                        parentMethod.setStatic(true);
                    } else if (entry == null && parameterIndex == 1 && !parentMethod.isStatic())
                    {
                        LOGGER.warn("Could not find a known already imported method that belongs to: " + parentMethod);
                        LOGGER.warn("Falling back to index based detection and marking the method as not static, since index 1 is present");
                    }

                    final boolean methodIsStatic = entry == null ? parentMethod.isStatic() : entry.isStatic();

                    final MethodDesc desc = new MethodDesc(parentMethod.getDescriptor());

                    int workingIndex = methodIsStatic ? 0 : 1;
                    String type = desc.getArgs().get(0); // Can do this, since we are parsing a parameter for sure.
                    for (int i = 0; i < (desc.getArgs().size() - 1) && workingIndex != parameterIndex ; i++){
                        type = desc.getArgs().get(i);
                        workingIndex++;
                        if (type.equals("J") || type.equals("D")) //Long and doubles are double size. So add another index.
                            workingIndex++;
                        type = desc.getArgs().get(i+1); //Get the next type. If we reached the working index. This will make sure the right type is selected.
                    }

                    if (workingIndex != parameterIndex)
                    {
                        LOGGER.warn("Failed to find the parameter in the descriptor: " + entry.getDescriptor() + " for index: " + parameterIndex);
                        return null;
                    }

                    return new ExternalMapping(
                      parentMethod.getInput().replace("method", "param") + "_" + parameterIndex,
                            components[3],
                            ExternalMappableType.PARAMETER,
                            releaseName.split("\\+")[0],
                            releaseName,
                            parentClass.getOutput(),
                            parentMethod.getOutput(),
                            parentMethod.getDescriptor(),
                            type,
                            null,
                            null,
                            parameterIndex,
                            false
                    );
                },
                line -> {
                    if (!line.startsWith("\t"))
                        return null;

                    final String workingLine = line.trim();
                    if (!workingLine.startsWith("c"))
                        return null;

                    final String[] components = line.split("\t");
                    if (components.length <= 1)
                        return null;

                    final String[] relevantComponents = Arrays.copyOfRange(components, 1, components.length);
                    return String.join("\t", relevantComponents);
                },
                IContextualParsingPostProcessor.NOOP, Constants.YARN_MAPPING_NAME
        );
    }
}
