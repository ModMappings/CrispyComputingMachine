package org.modmappings.crispycomputingmachine.processors.intermediary;

import com.google.common.collect.Lists;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualCommentParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParameterParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParsingPostProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class IntermediaryMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    protected IntermediaryMappingParsingProcessor(VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager) {
        super(
                (releaseName) -> Lists.asList(Path.of(releaseName, Constants.INTERMEDIARY_WORKING_DIR, "mappings", "mappings.tiny"), new Path[0]),
                (line, releaseName) -> {
                    if (!line.startsWith("CLASS"))
                        return null;

                    final String[] components = line.split("\t");
                    String parentClassOut = null;
                    if (components[2].contains("$"))
                        parentClassOut = components[2].substring(0, components[2].indexOf("$"));

                    return new ExternalMapping(
                            components[1],
                            components[2],
                            ExternalMappableType.CLASS,
                            releaseName,
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
                    if (!line.startsWith("METHOD"))
                        return null;

                    final String[] components = line.split("\t");

                    final String parentClassOut = parentClass.getOutput();

                    return new ExternalMapping(
                            components[3],
                            components[4],
                            ExternalMappableType.METHOD,
                            releaseName,
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            null,
                            components[2],
                            null,
                            null,
                            false);
                },
                (parentClass, line, releaseName) -> {
                    if (!line.startsWith("FIELD"))
                        return null;

                    final String[] components = line.split("\t");

                    final String parentClassOut = parentClass.getOutput();

                    return new ExternalMapping(
                            components[3],
                            components[4],
                            ExternalMappableType.FIELD,
                            releaseName,
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            components[2],
                            null,
                            null,
                            null,
                            false);
                },
                IContextualParameterParser.NOOP,
                IContextualCommentParser.NOOP,
                (classes, methods, fields, parameters) -> {
                    classes.forEach(classMapping -> {
                        final MappingCacheEntry classInVanilla = vanillaAndExternalMappingCacheManager.getClassViaInput(classMapping.getInput());
                        if(classInVanilla == null)
                            return;

                        final String classOutputInVanilla = classInVanilla.getOutput();
                        final List<MappingCacheEntry> constructorsInVanilla = vanillaAndExternalMappingCacheManager.getConstructorsForClass(classOutputInVanilla);

                        constructorsInVanilla.forEach(constructor -> {
                            methods.add(new ExternalMapping(
                                    constructor.getInput(),
                                    constructor.getOutput(),
                                    ExternalMappableType.METHOD,
                                    classMapping.getGameVersion(),
                                    classMapping.getReleaseName(),
                                    classMapping.getOutput(),
                                    null,
                                    null,
                                    null,
                                    constructor.getDescriptor(),
                                    null,
                                    null,
                                    false
                                    )
                            );
                        });
                    });
                },
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
