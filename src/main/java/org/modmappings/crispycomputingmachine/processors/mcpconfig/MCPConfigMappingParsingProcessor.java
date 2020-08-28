package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualCommentParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParameterParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParsingPostProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParsingPreProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodDesc;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MCPConfigMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    private static Map<String, Map<String, String>> constructors = Maps.newHashMap();
    private static List<String> staticMethodNames = Lists.newArrayList();

    protected MCPConfigMappingParsingProcessor() {
        super(
                        (releaseName) -> Lists.asList(Paths.get(releaseName, Constants.MCP_CONFIG_WORKING_DIR, "config", "joined.tsrg"), new Path[0]),
                        fileToBeProcessed -> {
                            final File parentDirectory = fileToBeProcessed.getParentFile();
                            final File staticMethodsFile = new File(parentDirectory, "static_methods.txt");
                            try (InputStream in = new FileInputStream(staticMethodsFile)) {
                                staticMethodNames = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                                                .filter(l -> !l.isEmpty()) //Remove Empty lines
                                                .collect(Collectors.toList());
                            }
                            catch (Exception e) {
                                throw new IllegalStateException("Failed to parse static methods.", e);
                            }

                            final File constructorsFile = new File(parentDirectory, "constructors.txt");
                            try (InputStream in = new FileInputStream(constructorsFile)) {
                                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                                                                    .filter(l -> !l.isEmpty()) //Remove Empty lines
                                                                    .map(l -> l.split(" "))
                                                                    .filter(l -> l.length == 3)
                                                .forEach(l -> {
                                                    constructors.computeIfAbsent(l[1], _ignored -> new HashMap<>()).put(l[2], l[0]);
                                                });
                            }
                            catch (Exception e) {
                                throw new IllegalStateException("Failed to parse constructors.", e);
                            }
                        },
                        (line, releaseName) -> {
                            if (line.startsWith("\t")) {
                                return null;
                            }

                            final String[] components = line.split(" ");
                            if (components.length != 2) {
                                return null;
                            }

                            String parentClassOut = null;
                            if (components[1].contains("$")) {
                                parentClassOut = components[1].substring(0, components[1].lastIndexOf("$"));
                            }

                            return new ExternalMapping(
                                            components[0],
                                            components[1],
                                            ExternalMappableType.CLASS,
                                            releaseName.split("-")[0],
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
                            if (!line.startsWith("\t")) {
                                return null;
                            }

                            final String workingLine = line.trim();
                            final String[] components = workingLine.split(" ");

                            if (components.length != 3) {
                                return null;
                            }

                            final String parentClassOut = parentClass.getOutput();

                            return new ExternalMapping(
                                            components[0],
                                            components[2],
                                            ExternalMappableType.METHOD,
                                            releaseName.split("-")[0],
                                            releaseName,
                                            parentClassOut,
                                            null,
                                            null,
                                            null,
                                            components[1],
                                            null, null, staticMethodNames.contains(components[2]));
                        },
                        (parentClass, line, releaseName) -> {
                            if (!line.startsWith("\t")) {
                                return null;
                            }

                            final String workingLine = line.trim();
                            final String[] components = workingLine.split(" ");

                            if (components.length != 2) {
                                return null;
                            }

                            final String parentClassOut = parentClass.getOutput();


                            return new ExternalMapping(
                                            components[0],
                                            components[1],
                                            ExternalMappableType.FIELD,
                                            releaseName.split("-")[0],
                                            releaseName,
                                            parentClassOut,
                                            null,
                                            null,
                                            "*",
                                            null,
                                            null,
                                            null, false);
                        },
                        IContextualParameterParser.NOOP,
                        IContextualCommentParser.NOOP,
                        (classes, methods, fields, parameters) -> {
                            methods.removeIf(evm -> {
                                return evm.getInput().equals(evm.getOutput()) || evm.getInput().startsWith("access$");
                            });

                            final Map<String, ExternalMapping> outputClasses = Maps.newConcurrentMap();
                            classes.parallelStream().forEach(cl -> outputClasses.put(cl.getOutput(), cl));

                            classes.parallelStream()
                                            .filter(evm -> constructors.containsKey(evm.getOutput()))
                                            .flatMap(evm -> constructors.get(evm.getOutput())
                                                            .entrySet().stream().map(e -> {
                                                final MethodDesc desc = new MethodDesc(e.getKey());
                                                final MethodDesc obfedDesc = desc.remap(clz -> Optional.ofNullable(outputClasses.get(clz)).map(ExternalMapping::getInput));

                                                return new ExternalMapping(
                                                                "<init>",
                                                                "func_" + e.getValue() + "_<init>",
                                                                ExternalMappableType.METHOD,
                                                                evm.getGameVersion(),
                                                                evm.getReleaseName(),
                                                                evm.getOutput(),
                                                                null,
                                                                null,
                                                                null,
                                                                obfedDesc.toString(),
                                                                null,
                                                                null,
                                                                false);
                                            })
                                            ).forEach(methods::add);

                            methods.stream()
                                            .filter(externalMapping -> !externalMapping.getInput().equals(externalMapping.getOutput()) &&
                                                                                       !externalMapping.getOutput()
                                                                                                        .startsWith("access$")) //Exclude any none mapped methods, and exclude synthetic accessors.
                                            .flatMap(externalMapping -> {
                                                final MethodDesc desc = new MethodDesc(externalMapping.getDescriptor());
                                                final AtomicInteger index = new AtomicInteger(externalMapping.isStatic() ? 0 : 1);

                                                return desc.getArgs().stream().map(argType -> {
                                                    String outputMapping = externalMapping.getOutput();
                                                    String[] outputComponents = outputMapping.split("_");
                                                    if (!outputMapping.contains("<init>"))
                                                    {
                                                        if (outputComponents.length > 1) {
                                                            outputMapping = "p_" + outputComponents[1] + "_" + index.get() + "_";
                                                        }
                                                    }
                                                    else
                                                    {
                                                        if (outputComponents.length > 1) {
                                                            outputMapping = "p_i" + outputComponents[1] + "_" + index.get() + "_";
                                                        }
                                                    }

                                                    final ExternalMapping mapping = new ExternalMapping(
                                                                    externalMapping.getInput() + "_" + index.get(),
                                                                    outputMapping,
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
                        Constants.MCP_CONFIG_MAPPING_NAME,
                        false
        );
    }
}
