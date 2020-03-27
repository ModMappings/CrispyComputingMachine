package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IntermediaryMappingFileExtractor implements ItemProcessor<String, List<ExternalMapping>> {

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public List<ExternalMapping> process(final String item) throws Exception {
        final File workingDirectoryFile = workingDirectory.getFile();
        workingDirectoryFile.mkdirs();
        final File versionWorkingDirectory = new File(workingDirectoryFile, item);
        versionWorkingDirectory.mkdirs();
        final File unzippingMappingJarTarget = new File(versionWorkingDirectory, "intermediary");
        final File mappingsDirectory = new File(unzippingMappingJarTarget,"mappings");
        final File mappingsTinyFile = new File(mappingsDirectory, "mappings.tiny");

        try (InputStream in = new FileInputStream(mappingsTinyFile)) {
            List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .skip(1) //Skip the damn header.
                    .filter(l -> !l.isEmpty()) //Remove Empty lines
                    .collect(Collectors.toList());

            final Map<String, ExternalMapping> classes = lines.stream().parallel()
                    .filter(line -> line.startsWith("CLASS"))
                    .map(line -> {
                        final String[] components = line.split("\t");
                        String parentClassOut = null;
                        if (components[2].contains("$"))
                            parentClassOut = components[2].substring(0, components[2].indexOf("$"));

                        return new ExternalMapping(
                                components[1],
                                components[2],
                                ExternalMappableType.CLASS,
                                item,
                                item,
                                parentClassOut,
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                    })
                    .collect(Collectors.toMap(ExternalMapping::getInput, Function.identity()));

            final List<ExternalMapping> fields = lines.parallelStream()
                    .filter(line -> line.startsWith("FIELD"))
                    .map(line -> {
                        final String[] components = line.split("\t");

                        final String parentClassOut = classes.get(components[1]).getOutput();

                        return new ExternalMapping(
                                components[3],
                                components[4],
                                ExternalMappableType.FIELD,
                                item,
                                item,
                                parentClassOut,
                                null,
                                null,
                                components[2],
                                null,
                                null
                        );
                        }
                    ).collect(Collectors.toList());

            final List<ExternalMapping> methods = lines.parallelStream()
                    .filter(line -> line.startsWith("METHOD"))
                    .map(line -> {
                        final String[] components = line.split("\t");

                        final String parentClassOut = classes.get(components[1]).getOutput();

                        return new ExternalMapping(
                                components[3],
                                components[4],
                                ExternalMappableType.METHOD,
                                item,
                                item,
                                parentClassOut,
                                null,
                                null,
                                null,
                                components[2],
                                null
                        );
                    }).collect(Collectors.toList());

            final List<ExternalMapping> results = new ArrayList<>(classes.values());
            results.addAll(methods);
            results.addAll(fields);

            return results;
        }
    }
}
