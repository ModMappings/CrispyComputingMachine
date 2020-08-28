package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import com.google.common.collect.Lists;
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

public abstract class AbstractContextualMappingParsingProcessor implements ItemProcessor<String, List<ExternalMapping>> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    private final Function<String, List<Path>> pathsExtractor;
    private final IContextualParsingPreProcessor preProcessor;
    private final IContextualClassParser classParser;
    private final IContextualMethodParser methodParser;
    private final IContextualFieldParser fieldParser;
    private final IContextualParameterParser parameterParser;
    private final IContextualCommentParser commentParser;
    private final IContextualParsingPostProcessor postProcessor;
    private final String mappingTypeName;
    private final boolean skipHeader;

    protected AbstractContextualMappingParsingProcessor(
                    final Function<String, List<Path>> pathsExtractor,
                    final IContextualParsingPreProcessor preProcessor,
                    final IContextualClassParser classParser,
                    final IContextualMethodParser methodParser,
                    final IContextualFieldParser fieldParser,
                    final IContextualParameterParser parameterParser,
                    final IContextualCommentParser commentParser,
                    final IContextualParsingPostProcessor postProcessor,
                    final String mappingTypeName,
                    boolean skipHeader) {
        this.pathsExtractor = pathsExtractor;
        this.preProcessor = preProcessor;
        this.classParser = classParser;
        this.methodParser = methodParser;
        this.fieldParser = fieldParser;
        this.parameterParser = parameterParser;
        this.commentParser = commentParser;
        this.postProcessor = postProcessor;
        this.mappingTypeName = mappingTypeName;
        this.skipHeader = skipHeader;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public List<ExternalMapping> process(@NotNull final String releaseName) throws Exception {
        final List<Path> pathsToParse = this.pathsExtractor.apply(releaseName);
        final File workingDirectoryFile = workingDirectory.getFile();
        workingDirectoryFile.mkdirs();

        final Path workingDirectoryPath = workingDirectoryFile.toPath();

        final List<ExternalMapping> classes = Lists.newArrayList();
        final List<ExternalMapping> methods = Lists.newArrayList();
        final List<ExternalMapping> fields = Lists.newArrayList();
        final List<ExternalMapping> parameters = Lists.newArrayList();

        pathsToParse.forEach(path -> {
            final Path targetFilePath = workingDirectoryPath.resolve(path);
            final File targetFile = targetFilePath.toFile();
            if (!targetFile.exists())
                throw new IllegalStateException(String.format("The path:%s does not exist.", path));

            preProcessor.processFile(targetFile);

            try (InputStream in = new FileInputStream(targetFile)) {
                List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                        .skip(skipHeader ? 1 : 0) //Skip the damn header.
                        .filter(l -> !l.isEmpty()) //Remove Empty lines
                        .collect(Collectors.toList());

                final List<ExternalMapping> fileClasses = Lists.newArrayList();
                final List<ExternalMapping> fileMethods = Lists.newArrayList();
                final List<ExternalMapping> fileFields = Lists.newArrayList();
                final List<ExternalMapping> fileParameters = Lists.newArrayList();
                ExternalMapping targetClass = null;
                ExternalMapping targetMethod = null;
                ExternalMapping lastResult = null;
                for (String s : lines) {
                    if (lastResult != null)
                    {
                        final String parameterCandidate = this.commentParser.parse(s);
                        if (parameterCandidate != null)
                        {
                            lastResult.setDocumentation(parameterCandidate);
                        }
                    }

                    final ExternalMapping classCandidate = this.classParser.parse(s, releaseName);
                    if (classCandidate != null)
                    {
                        targetClass = classCandidate;
                        fileClasses.add(targetClass);
                        lastResult = targetClass;
                        continue;
                    }

                    //Guarantee a class is available before continuing processing.
                    if (targetClass == null)
                        throw new IllegalStateException(String.format("%s mappings in file: %s do not start with a class.", mappingTypeName, path));

                    final ExternalMapping fieldCandidate = this.fieldParser.parse(targetClass, s, releaseName);
                    if (fieldCandidate != null)
                    {
                        fileFields.add(fieldCandidate);
                        lastResult = fieldCandidate;
                        continue;
                    }

                    //We have a method already. Lets check if this line, which followed the method line is a parameter or not.
                    if (targetMethod != null) {
                        final ExternalMapping parameterCandidate = this.parameterParser.parse(targetClass, targetMethod, s, releaseName);
                        if (parameterCandidate != null)
                        {
                            fileParameters.add(parameterCandidate);
                            lastResult = parameterCandidate;
                            continue;
                        }

                        //No further parameters. So lets clear the memory.
                        targetMethod = null;
                    }

                    final ExternalMapping methodCandidate = this.methodParser.parse(targetClass, s, releaseName);
                    if (methodCandidate != null)
                    {
                        targetMethod = methodCandidate;
                        fileMethods.add(targetMethod);
                        lastResult = methodCandidate;
                        continue;
                    }

                    lastResult = null;
                }

                this.postProcessor.processFile(fileClasses, fileMethods, fileFields, fileParameters);

                classes.addAll(fileClasses);
                methods.addAll(fileMethods);
                fields.addAll(fileFields);
                parameters.addAll(fileParameters);
            } catch (IOException e) {
                LOGGER.warn(String.format("Failed to parse file: %s for mappings: %s", path, mappingTypeName), e);
                throw new IllegalStateException("Failed to parse file.", e);
            }
        });

        final List<ExternalMapping> results = new ArrayList<>();
        results.addAll(classes);
        results.addAll(methods);
        results.addAll(fields);
        results.addAll(parameters);
        return results;
    }
}
