package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractSimpleMappingParsingProcessor implements ItemProcessor<String, List<ExternalMapping>>
{

    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private final Function<String, List<Path>> pathsExtractor;
    private final ISimpleClassParser           classParser;
    private final IClassesPostProcessor        classesPostProcessor;
    private final IClassesPreProcessor         classesPreProcessor;
    private final ISimpleMethodParser          methodParser;
    private final IMethodsPostProcessor        methodsPostProcessor;
    private final IMethodsPreProcessor         methodsPreProcessor;
    private final ISimpleFieldParser           fieldParser;
    private final IFieldsPostProcessor         fieldsPostProcessor;
    private final IFieldsPreProcessor          fieldsPreProcessor;
    private final ISimpleParameterParser       parameterParser;
    private final IParametersPostProcessor     parametersPostProcessor;
    private final IParametersPreProcessor      parametersPreProcessor;
    private final String                       mappingTypeName;

    protected AbstractSimpleMappingParsingProcessor(
      final Function<String, List<Path>> pathsExtractor,
      final IClassesPreProcessor classesPreProcessor,
      final ISimpleClassParser classParser,
      final IClassesPostProcessor classesPostProcessor,
      final IMethodsPreProcessor methodsPreProcessor,
      final ISimpleMethodParser methodParser,
      final IMethodsPostProcessor methodsPostProcessor,
      final IFieldsPreProcessor fieldsPreProcessor,
      final ISimpleFieldParser fieldParser,
      final IFieldsPostProcessor fieldsPostProcessor,
      final IParametersPreProcessor parametersPreProcessor,
      final ISimpleParameterParser parameterParser,
      final IParametersPostProcessor parametersPostProcessor,
      final String mappingTypeName)
    {
        this.pathsExtractor = pathsExtractor;
        this.classParser = classParser;
        this.classesPostProcessor = classesPostProcessor;
        this.classesPreProcessor = classesPreProcessor;
        this.methodParser = methodParser;
        this.methodsPostProcessor = methodsPostProcessor;
        this.methodsPreProcessor = methodsPreProcessor;
        this.fieldParser = fieldParser;
        this.fieldsPostProcessor = fieldsPostProcessor;
        this.fieldsPreProcessor = fieldsPreProcessor;
        this.parameterParser = parameterParser;
        this.parametersPostProcessor = parametersPostProcessor;
        this.parametersPreProcessor = parametersPreProcessor;
        this.mappingTypeName = mappingTypeName;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public List<ExternalMapping> process(@NotNull final String item) throws Exception
    {
        final List<Path> pathsToParse = this.pathsExtractor.apply(item);
        final File workingDirectoryFile = workingDirectory.getFile();
        workingDirectoryFile.mkdirs();

        final Path workingDirectoryPath = workingDirectoryFile.toPath();

        final Set<ExternalMapping> classes = Sets.newConcurrentHashSet();
        final Set<ExternalMapping> methods = Sets.newConcurrentHashSet();
        final Set<ExternalMapping> fields = Sets.newConcurrentHashSet();
        final Set<ExternalMapping> parameters = Sets.newConcurrentHashSet();

        final Map<Path, List<String>> pathToLinesMap = Maps.newHashMap();

        pathsToParse.forEach(path -> {
            final Path targetFilePath = workingDirectoryPath.resolve(path);
            final File targetFile = targetFilePath.toFile();
            if (!targetFile.exists())
            {
                throw new IllegalStateException(String.format("The path:%s does not exist.", path));
            }

            try (InputStream in = new FileInputStream(targetFile))
            {
                List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                                       .skip(1) //Skip the damn header.
                                       .filter(l -> !l.isEmpty()) //Remove Empty lines
                                       .collect(Collectors.toList());

                pathToLinesMap.put(path, lines);
            }
            catch (IOException e)
            {
                LOGGER.warn(String.format("Failed to parse file: %s for mappings: %s", path, mappingTypeName), e);
                throw new IllegalStateException("Failed to parse file.", e);
            }
        });

        this.classesPreProcessor.apply(item);

        pathToLinesMap.keySet().parallelStream().filter(this.classParser::acceptsFile).forEach(path -> {
            final List<String> lines = pathToLinesMap.get(path);
            final Set<ExternalMapping> fileClasses = lines.parallelStream()
                                                               .flatMap(line -> this.classParser.parse(line, item).stream())
                                                               .filter(Objects::nonNull)
                                                               .collect(Collectors.toSet());

            classes.addAll(fileClasses);
        });

        this.classesPostProcessor.apply(item, classes);

        this.methodsPreProcessor.apply(item, classes);

        pathToLinesMap.keySet().parallelStream().filter(this.methodParser::acceptsFile).forEach(path -> {
            final List<String> lines = pathToLinesMap.get(path);
            final Set<ExternalMapping> fileMethods = lines.parallelStream()
                                                               .flatMap(line -> this.methodParser.parse(classes, line, item).stream())
                                                               .collect(Collectors.toSet());

            methods.addAll(fileMethods);
        });

        this.methodsPostProcessor.apply(item, classes, methods);

        this.fieldsPreProcessor.apply(item, classes);

        pathToLinesMap.keySet().parallelStream().filter(this.fieldParser::acceptsFile).forEach(path -> {
            final List<String> lines = pathToLinesMap.get(path);
            final Set<ExternalMapping> fileFields = lines.parallelStream()
                                                              .flatMap(line -> this.fieldParser.parse(classes, line, item).stream())
                                                              .filter(Objects::nonNull)
                                                              .collect(Collectors.toSet());

            fields.addAll(fileFields);
        });

        this.fieldsPostProcessor.apply(item, classes, fields);

        this.parametersPreProcessor.apply(item, classes, methods);

        pathToLinesMap.keySet().parallelStream().filter(this.parameterParser::acceptsFile).forEach(path -> {
            final List<String> lines = pathToLinesMap.get(path);
            final Set<ExternalMapping> fileParameters = lines.parallelStream()
                                                                  .flatMap(line -> this.parameterParser.parse(methods, line, item).stream())
                                                                  .filter(Objects::nonNull)
                                                                  .collect(Collectors.toSet());

            parameters.addAll(fileParameters);
        });

        this.parametersPostProcessor.apply(item, classes, methods, parameters);

        final List<ExternalMapping> results = new ArrayList<>();
        results.addAll(classes);
        results.addAll(methods);
        results.addAll(fields);
        results.addAll(parameters);
        return results;
    }
}
