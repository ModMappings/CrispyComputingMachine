package org.modmappings.crispycomputingmachine.processors.mcp;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.cache.MCPConfigMappingCacheManager;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalDistribution;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.*;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.MethodRef;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractMCPMappingParsingProcessor extends AbstractSimpleMappingParsingProcessor
{
    private final MCPConfigMappingCacheManager mcpConfigMappingCacheManager;

    protected AbstractMCPMappingParsingProcessor(
      final String MCP_WORKING_DIR,
      final MCPConfigMappingCacheManager mcpConfigMappingCacheManager)
    {
        super(s -> List.of(
          Paths.get(s, MCP_WORKING_DIR, "methods.csv"),
          Paths.get(s, MCP_WORKING_DIR, "fields.csv"),
          Paths.get(s, MCP_WORKING_DIR, "params.csv")
          ),
          ISimpleClassParser.NOOP,
          (IClassesPostProcessor) (releaseName, classes) -> {
              final String gameVersionName = releaseName.split("-")[1];

              mcpConfigMappingCacheManager.getAllClasses()
                .parallelStream()
                .filter(entry -> entry.getGameVersionName().equals(gameVersionName)).map(entry -> new ExternalMapping(
                  entry.getOutput(),
                  entry.getOutput(),
                  ExternalMappableType.CLASS,
                  releaseName,
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
              .forEach(classes::add);
          },
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
                  final String release = releaseNameComponents[0];
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigMappingCacheManager.getAllMethods().stream()
                           .filter(method -> method.getOutput().equals(inputMapping))
                           .map(method -> {
                               final ExternalMapping mapping = new ExternalMapping(
                                 inputMapping,
                                 outputMapping,
                                 ExternalMappableType.METHOD,
                                 gameVersion,
                                 release,
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
          IMethodsPostProcessor.NOOP,
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
                      return List.of();

                  final String[] releaseNameComponents = releaseName.split("-");
                  final String release = releaseNameComponents[0];
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigMappingCacheManager.getAllFields()
                    .stream()
                    .filter(field -> field.getOutput().equals(inputMapping))
                    .map(field -> {
                        final ExternalMapping mapping = new ExternalMapping(
                          inputMapping,
                          outputMapping,
                          ExternalMappableType.FIELD,
                          gameVersion,
                          release,
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
          IFieldsPostProcessor.NOOP,
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
                      return List.of();

                  final Map<MethodRef, ExternalMapping> existingMethodMappings =
                    methods.parallelStream()
                    .collect(Collectors.toMap(externalMapping -> new MethodRef(
                      externalMapping.getParentClassMapping(),
                      externalMapping.getOutput(),
                      externalMapping.getDescriptor()
                    ),
                      Function.identity()));

                  final String[] releaseNameComponents = releaseName.split("-");
                  final String release = releaseNameComponents[0];
                  final String gameVersion = releaseNameComponents[1];

                  final String[] lineComponents = line.split(",");

                  final String inputMapping = lineComponents[0];
                  final String outputMapping = lineComponents[1];

                  return mcpConfigMappingCacheManager.getAllParameters().parallelStream()
                    .filter(parameter -> parameter.getOutput().equals(inputMapping))
                    .map(parameter -> {
                        final MethodRef ownerRef = new MethodRef(
                          parameter.getParentClassOutput(),
                          parameter.getParentMethodOutput(),
                          parameter.getParentMethodDescriptor()
                        );

                        if (existingMethodMappings.get(ownerRef) == null)
                            System.out.println("HELLO");

                        final ExternalMapping mapping = new ExternalMapping(
                          inputMapping,
                          outputMapping,
                          ExternalMappableType.PARAMETER,
                          gameVersion,
                          release,
                          parameter.getParentClassOutput(),
                          existingMethodMappings.get(ownerRef).getOutput(),
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
          IParametersPostProcessor.NOOP,
          Constants.MCP_MAPPING_NAME);
        this.mcpConfigMappingCacheManager = mcpConfigMappingCacheManager;
    }
}
