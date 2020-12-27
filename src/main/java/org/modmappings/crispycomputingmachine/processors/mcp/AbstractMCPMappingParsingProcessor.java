package org.modmappings.crispycomputingmachine.processors.mcp;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.cache.MappingCacheEntry;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalDistribution;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.*;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodRef;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractMCPMappingParsingProcessor extends AbstractSimpleMappingParsingProcessor
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static Collection<MappingCacheEntry>   mcpConfigClasses     = Collections.emptyList();
    private static Collection<MappingCacheEntry>   mcpConfigMethods     = Collections.emptyList();
    private static Collection<MappingCacheEntry>   mcpConfigFields      = Collections.emptyList();
    private static Collection<MappingCacheEntry>   mcpConfigParameters  = Collections.emptyList();
    private static Map<MethodRef, ExternalMapping> parsedMethodMappings = Maps.newHashMap();

    protected AbstractMCPMappingParsingProcessor(
      final String MCP_WORKING_DIR,
      final MCPConfigMappingCacheManager mcpConfigMappingCacheManager)
    {
        super(s -> List.of(
          Paths.get(s, MCP_WORKING_DIR, "methods.csv"),
          Paths.get(s, MCP_WORKING_DIR, "fields.csv"),
          Paths.get(s, MCP_WORKING_DIR, "params.csv")
          ),
          releaseName -> mcpConfigClasses = mcpConfigMappingCacheManager.getAllClasses(),
          ISimpleClassParser.NOOP,
          (releaseName, classes) -> {
              final String gameVersionName = releaseName.split("-")[1];

              mcpConfigClasses
                .parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersionName)).map(entry -> new ExternalMapping(
                entry.getOutput(),
                entry.getOutput(),
                ExternalMappableType.CLASS,
                gameVersionName,
                releaseName,
                entry.getParentClassOutput(),
                null,
                null,
                null,
                null,
                null,
                null,
                entry.isStatic()
              ))
                .peek(mapping -> mapping.setLocked(true)) // All classes in MCP are locked.
                .forEach(classes::add);
          },
          (releaseName, classes) -> mcpConfigMethods = mcpConfigMappingCacheManager.getAllMethods(),
          new ISimpleMethodParser()
          {
              @Override
              public boolean acceptsFile(@NotNull final Path path)
              {
                  return path.endsWith("methods.csv");
              }

              @Override
              public @NotNull Collection<ExternalMapping> parse(final @NotNull Set<ExternalMapping> classes, final @NotNull String line, final String releaseName)
              {
                  if (line.contentEquals("searge,name,side,desc"))
                  {
                      return List.of();
                  }

                  final String[] releaseNameComponents = releaseName.split("-");
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigMethods.stream()
                           .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                           .filter(method -> method.getOutput().equals(inputMapping))
                           .map(method -> {
                               final ExternalMapping mapping = new ExternalMapping(
                                 inputMapping,
                                 outputMapping,
                                 ExternalMappableType.METHOD,
                                 gameVersion,
                                 releaseName,
                                 method.getParentClassOutput(),
                                 "",
                                 "",
                                 "",
                                 method.getDescriptor(),
                                 "",
                                 -1,
                                 method.isStatic()
                               );

                               mapping.setDocumentation(lineComponents.length == 4 ? lineComponents[3] : "");
                               mapping.setExternalDistribution(ExternalDistribution.values()[Integer.parseInt(lineComponents[2])]);

                               return mapping;
                           })
                           .collect(Collectors.toList());
              }
          },
          (releaseName, classes, methods) -> {
              final String[] releaseNameComponents = releaseName.split("-");
              final String gameVersion = releaseNameComponents[1];

              final Set<String> knownMethods = methods.parallelStream().map(ExternalMapping::getInput).collect(Collectors.toSet());

              mcpConfigMethods.parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                .filter(method -> method.getInput().equals("<init>"))
                .forEach(constructorMethod -> {
                    methods.add(new ExternalMapping(
                      constructorMethod.getOutput(),
                      "<init>",
                      ExternalMappableType.METHOD,
                      gameVersion,
                      releaseName,
                      constructorMethod.getParentClassOutput(),
                      "",
                      "",
                      "",
                      constructorMethod.getDescriptor(),
                      "",
                      -1,
                      constructorMethod.isStatic()
                    ));
                });

              mcpConfigMethods.parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                .filter(method -> !method.getInput().equals("<init>"))
                .filter(method -> !knownMethods.contains(method.getOutput()))
                .forEach(noneMappedMethod -> {
                    methods.add(new ExternalMapping(
                      noneMappedMethod.getOutput(),
                      noneMappedMethod.getOutput(),
                      ExternalMappableType.METHOD,
                      gameVersion,
                      releaseName,
                      noneMappedMethod.getParentClassOutput(),
                      "",
                      "",
                      "",
                      noneMappedMethod.getDescriptor(),
                      "",
                      -1,
                      noneMappedMethod.isStatic()
                    ));
                });
          },
          (releaseName, classes) -> mcpConfigFields = mcpConfigMappingCacheManager.getAllFields(),
          new ISimpleFieldParser()
          {
              @Override
              public boolean acceptsFile(@NotNull final Path path)
              {
                  return path.endsWith("fields.csv");
              }

              @NotNull
              @Override
              public Collection<ExternalMapping> parse(final @NotNull Set<ExternalMapping> classes, final @NotNull String line, final String releaseName)
              {
                  if (line.contentEquals("searge,name,side,desc"))
                  {
                      return List.of();
                  }

                  final String[] releaseNameComponents = releaseName.split("-");
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigFields
                           .parallelStream()
                           .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                           .filter(field -> field.getOutput().equals(inputMapping))
                           .map(field -> {
                               final ExternalMapping mapping = new ExternalMapping(
                                 inputMapping,
                                 outputMapping,
                                 ExternalMappableType.FIELD,
                                 gameVersion,
                                 releaseName,
                                 field.getParentClassOutput(),
                                 "",
                                 "",
                                 field.getType(),
                                 "",
                                 "",
                                 -1,
                                 field.isStatic()
                               );

                               mapping.setDocumentation(lineComponents.length == 4 ? lineComponents[3] : "");
                               mapping.setExternalDistribution(ExternalDistribution.values()[Integer.parseInt(lineComponents[2])]);

                               return mapping;
                           })
                           .collect(Collectors.toList());
              }
          },
          (releaseName, classes, fields) -> {
              final String[] releaseNameComponents = releaseName.split("-");
              final String gameVersion = releaseNameComponents[1];

              final Set<String> knownFields = fields.parallelStream().map(ExternalMapping::getInput).collect(Collectors.toSet());

              mcpConfigFields.parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                .filter(method -> !knownFields.contains(method.getOutput()))
                .forEach(noneMappedField -> {
                    fields.add(new ExternalMapping(
                      noneMappedField.getOutput(),
                      noneMappedField.getOutput(),
                      ExternalMappableType.FIELD,
                      gameVersion,
                      releaseName,
                      noneMappedField.getParentClassOutput(),
                      "",
                      "",
                      noneMappedField.getType(),
                      "",
                      "",
                      -1,
                      noneMappedField.isStatic()
                    ));
                });
          },
          (releaseName, classes, methods) -> {
              mcpConfigParameters = mcpConfigMappingCacheManager.getAllParameters();
              parsedMethodMappings =
                methods.parallelStream()
                  .collect(Collectors.toMap(externalMapping -> new MethodRef(
                      externalMapping.getParentClassMapping(),
                      externalMapping.getInput(),
                      externalMapping.getDescriptor()
                    ),
                    Function.identity()));

              try {
                  //noinspection ResultOfMethodCallIgnored
                  methods.parallelStream()
                    .collect(Collectors.toMap(externalMapping -> new MethodRef(
                        externalMapping.getParentClassMapping(),
                        externalMapping.getOutput(),
                        externalMapping.getDescriptor()
                      ),
                      Function.identity()));
              } catch (Exception ex) {
                  LOGGER.error("Detected duplicate methods with the same signature and name in the same class. This is likely a mapping error. Proceed with caution!");
              }
          },
          new ISimpleParameterParser()
          {
              @Override
              public boolean acceptsFile(@NotNull final Path path)
              {
                  return path.endsWith("params.csv");
              }

              @NotNull
              @Override
              public Collection<ExternalMapping> parse(final @NotNull Set<ExternalMapping> methods, final @NotNull String line, final String releaseName)
              {
                  if (line.contentEquals("param,name,side"))
                  {
                      return List.of();
                  }

                  final String[] releaseNameComponents = releaseName.split("-");
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigParameters.parallelStream()
                           .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                           .filter(parameter -> parameter.getOutput().equals(inputMapping))
                           .map(parameter -> {
                               final MethodRef ownerRef = new MethodRef(
                                 parameter.getParentClassOutput(),
                                 parameter.getParentMethodOutput(),
                                 parameter.getParentMethodDescriptor()
                               );

                               final ExternalMapping mapping = new ExternalMapping(
                                 inputMapping,
                                 outputMapping,
                                 ExternalMappableType.PARAMETER,
                                 gameVersion,
                                 releaseName,
                                 parameter.getParentClassOutput(),
                                 parsedMethodMappings.get(ownerRef) == null ? parameter.getParentMethodOutput() : parsedMethodMappings.get(ownerRef).getOutput(),
                                 parameter.getParentMethodDescriptor(),
                                 parameter.getType(),
                                 "",
                                 "",
                                 parameter.getIndex(),
                                 false
                               );

                               mapping.setExternalDistribution(ExternalDistribution.values()[Integer.parseInt(lineComponents[2])]);

                               return mapping;
                           })
                           .collect(Collectors.toList());
              }
          },
          (releaseName, classes, methods, parameters) -> {
              final String[] releaseNameComponents = releaseName.split("-");
              final String gameVersion = releaseNameComponents[1];

              final Set<String> knownParameters = parameters.parallelStream().map(ExternalMapping::getInput).collect(Collectors.toSet());

              mcpConfigParameters.parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersion))
                .filter(method -> !knownParameters.contains(method.getOutput()))
                .forEach(noneMappedParameter -> {
                    final MethodRef ownerRef = new MethodRef(
                      noneMappedParameter.getParentClassOutput(),
                      noneMappedParameter.getParentMethodOutput(),
                      noneMappedParameter.getParentMethodDescriptor()
                    );

                    parameters.add(new ExternalMapping(
                      noneMappedParameter.getOutput(),
                      noneMappedParameter.getOutput(),
                      ExternalMappableType.PARAMETER,
                      gameVersion,
                      releaseName,
                      noneMappedParameter.getParentClassOutput(),
                      parsedMethodMappings.get(ownerRef) == null ? noneMappedParameter.getParentMethodOutput() : parsedMethodMappings.get(ownerRef).getOutput(),
                      noneMappedParameter.getParentMethodDescriptor(),
                      noneMappedParameter.getType(),
                      "",
                      "",
                      noneMappedParameter.getIndex(),
                      false
                    ));
                });
          },
          Constants.MCP_MAPPING_NAME);
    }
}
