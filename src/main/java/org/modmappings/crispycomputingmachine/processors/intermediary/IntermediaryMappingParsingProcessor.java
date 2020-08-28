package org.modmappings.crispycomputingmachine.processors.intermediary;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.cache.VanillaAndExternalMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualCommentParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParameterParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParsingPreProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodDesc;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Component
public class IntermediaryMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    protected IntermediaryMappingParsingProcessor(VanillaAndExternalMappingCacheManager vanillaAndExternalMappingCacheManager) {
        super(
                        (releaseName) -> Lists.asList(Path.of(releaseName, Constants.INTERMEDIARY_WORKING_DIR, "mappings", "mappings.tiny"), new Path[0]),
                        IContextualParsingPreProcessor.NOOP,
                        (line, releaseName) -> {
                            if (!line.startsWith("CLASS")) {
                                return null;
                            }

                            final String[] components = line.split("\t");
                            String parentClassOut = null;
                            if (components[2].contains("$")) {
                                parentClassOut = components[2].substring(0, components[2].lastIndexOf("$"));
                            }

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
                            if (!line.startsWith("METHOD")) {
                                return null;
                            }

                            final String[] components = line.split("\t");

                            final String parentClassOut = parentClass.getOutput();
                            final String parentClassVanillaOut = vanillaAndExternalMappingCacheManager
                                            .getClassViaInput(parentClass.getInput()).getOutput();

                            final boolean isStatic = vanillaAndExternalMappingCacheManager
                                            .getMethodViaInput(components[3], parentClassVanillaOut, components[2]).isStatic();

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
                                            isStatic);
                        },
                        (parentClass, line, releaseName) -> {
                            if (!line.startsWith("FIELD")) {
                                return null;
                            }

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
                            final Map<String, ExternalMapping> inputClasses = Maps.newConcurrentMap();
                            final Map<Tuple3<String, String, String>, ExternalMapping> methodMappingMap = Maps.newConcurrentMap();

                            classes.parallelStream().forEach(cl -> inputClasses.put(cl.getInput(), cl));
                            methods.parallelStream().forEach(em -> methodMappingMap.put(Tuples.of(em.getParentClassMapping(), em.getInput(), em.getDescriptor()), em));

                            classes.forEach(classMapping -> {
                                final MappingCacheEntry classInVanilla = vanillaAndExternalMappingCacheManager.getClassViaInput(classMapping.getInput());
                                if (classInVanilla == null) {
                                    return;
                                }

                                final String classOutputInVanilla = classInVanilla.getOutput();
                                final List<MappingCacheEntry> methodsInVanilla = vanillaAndExternalMappingCacheManager.getAllMethodForClass(classOutputInVanilla);

                                methodsInVanilla.stream()
                                                .filter(mce -> !methodMappingMap.containsKey(Tuples.of(classMapping.getOutput(), mce.getInput(), mce.getOriginalDescriptor())))
                                                .map(
                                                                method -> {
                                                                    String inputOfMapping = method.getInput();
                                                                    String outputOfMapping = method.getInput();

                                                                    LinkedList<UUID> overrideMethodsToCheck = new LinkedList<>(vanillaAndExternalMappingCacheManager.getOverrides(method.getGameVersionName(), method.getVersionedMappableId()));
                                                                    while(!overrideMethodsToCheck.isEmpty())
                                                                    {
                                                                        final UUID overriddenMethod = overrideMethodsToCheck.removeFirst();
                                                                        final MappingCacheEntry overriddenVanillaMethodCacheEntry = vanillaAndExternalMappingCacheManager.getMethodCacheEntry(overriddenMethod);

                                                                        if (overriddenVanillaMethodCacheEntry != null)
                                                                        {
                                                                            final MappingCacheEntry vanillaClass = vanillaAndExternalMappingCacheManager.getClassViaOutput(overriddenVanillaMethodCacheEntry.getParentClassOutput());
                                                                            final ExternalMapping intermediarySuperClass = inputClasses.get(vanillaClass.getInput());

                                                                            final ExternalMapping overriddenMapping = methodMappingMap.get(Tuples.of(intermediarySuperClass.getOutput(), overriddenVanillaMethodCacheEntry.getInput(), overriddenVanillaMethodCacheEntry.getOriginalDescriptor()));

                                                                            if (overriddenMapping != null)
                                                                            {
                                                                                inputOfMapping = overriddenMapping.getInput();
                                                                                outputOfMapping = overriddenMapping.getOutput();
                                                                                break;
                                                                            }
                                                                        }

                                                                        vanillaAndExternalMappingCacheManager.getOverrides(method.getGameVersionName(), overriddenMethod).forEach(overrideMethodsToCheck::addLast);
                                                                    }


                                                                    return new ExternalMapping(
                                                                                    inputOfMapping,
                                                                                    outputOfMapping,
                                                                                    ExternalMappableType.METHOD,
                                                                                    classMapping.getGameVersion(),
                                                                                    classMapping.getReleaseName(),
                                                                                    classMapping.getOutput(),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    method.getOriginalDescriptor(),
                                                                                    null,
                                                                                    null,
                                                                                    method.isStatic()
                                                                    );
                                                                })
                                                .forEach(em -> {
                                                    methods.add(em);
                                                    methodMappingMap.put(Tuples.of(em.getParentClassMapping(), em.getInput(), em.getDescriptor()), em);
                                                });
                            });

                            methods.stream().flatMap(externalMapping -> {
                                final MethodDesc desc = new MethodDesc(externalMapping.getDescriptor());
                                final AtomicInteger index = new AtomicInteger(externalMapping.isStatic() ? 0 : 1);

                                return desc.getArgs().stream().map(argType -> {
                                    final ExternalMapping mapping = new ExternalMapping(
                                                    externalMapping.getInput().replace("method", "param") + "_" + index.get(),
                                                    externalMapping.getOutput().replace("method", "param") + "_" + index.get(),
                                                    ExternalMappableType.PARAMETER,
                                                    externalMapping.getGameVersion(),
                                                    externalMapping.getReleaseName(),
                                                    externalMapping.getParentClassMapping(),
                                                    externalMapping.getOutput(),
                                                    externalMapping.getDescriptor(),
                                                    argType,
                                                    null,
                                                    null,
                                                    index.get(),
                                                    false
                                    );

                                    if (argType.equals("D") || argType.equals("J")) {
                                        index.incrementAndGet();
                                    }
                                    index.incrementAndGet();

                                    return mapping;
                                });
                            }).forEach(parameters::add);
                        },
                        Constants.INTERMEDIARY_MAPPING_NAME,
                        true
        );
    }
}
