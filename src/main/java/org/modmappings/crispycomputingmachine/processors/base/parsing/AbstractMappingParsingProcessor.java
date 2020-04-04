package org.modmappings.crispycomputingmachine.processors.base.parsing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractMappingParsingProcessor implements ItemProcessor<String, List<ExternalMapping>> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private final Function<String, List<Path>> pathsExtractor;
    private final IClassParser classParser;
    private final IMethodParser methodParser;
    private final IFieldParser fieldParser;
    private final IParameterParser parameterParser;
    private final String mappingTypeName;

    protected AbstractMappingParsingProcessor(final Function<String, List<Path>> pathsExtractor, final IClassParser classParser, final IMethodParser methodParser, final IFieldParser fieldParser, final IParameterParser parameterParser, final String mappingTypeName) {
        this.pathsExtractor = pathsExtractor;
        this.classParser = classParser;
        this.methodParser = methodParser;
        this.fieldParser = fieldParser;
        this.parameterParser = parameterParser;
        this.mappingTypeName = mappingTypeName;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public List<ExternalMapping> process(@NotNull final String item) throws Exception {
        final List<Path> pathsToParse = this.pathsExtractor.apply(item);
        final File workingDirectoryFile = workingDirectory.getFile();
        workingDirectoryFile.mkdirs();

        final Path workingDirectoryPath = workingDirectoryFile.toPath();

        final Map<String, ExternalMapping> classes = Maps.newHashMap();
        final Map<String, ExternalMapping> methods = Maps.newHashMap();
        final List<ExternalMapping> fields = Lists.newArrayList();
        final List<ExternalMapping> parameters = Lists.newArrayList();

        pathsToParse.forEach(path -> {
            final Path targetFilePath = workingDirectoryPath.resolve(path);
            final File targetFile = targetFilePath.toFile();
            if (!targetFile.exists())
                throw new IllegalStateException(String.format("The path:%s does not exist.", path));

            try (InputStream in = new FileInputStream(targetFile)) {
                List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                        .skip(1) //Skip the damn header.
                        .filter(l -> !l.isEmpty()) //Remove Empty lines
                        .collect(Collectors.toList());

                final Map<String, ExternalMapping> fileClasses = lines.parallelStream()
                        .map(line -> this.classParser.parse(line, item))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(ExternalMapping::getInput, Function.identity()));

                classes.putAll(fileClasses);

                final Map<String, ExternalMapping> fileMethods = lines.parallelStream()
                        .map(line -> this.methodParser.parse(classes, line, item))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(ExternalMapping::getInput, Function.identity(), (l, r) -> r));

                methods.putAll(fileMethods);

                final List<ExternalMapping> fileFields = lines.parallelStream()
                        .map(line -> this.fieldParser.parse(classes, line, item))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                fields.addAll(fileFields);

                final List<ExternalMapping> fileParameters = lines.parallelStream()
                        .map(line -> this.parameterParser.parse(methods, line, item))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                parameters.addAll(fileParameters);
            } catch (IOException e) {
                LOGGER.warn(String.format("Failed to parse file: %s for mappings: %s", path, mappingTypeName), e);
                throw new IllegalStateException("Failed to parse file.", e);
            }
        });

        final List<ExternalMapping> results = new ArrayList<>();
        results.addAll(classes.values());
        results.addAll(methods.values());
        results.addAll(fields);
        results.addAll(parameters);
        return results;
    }
}
