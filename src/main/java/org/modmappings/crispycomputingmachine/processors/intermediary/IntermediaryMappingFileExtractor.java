package org.modmappings.crispycomputingmachine.processors.intermediary;

import com.google.common.collect.Lists;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.*;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IntermediaryMappingFileExtractor extends AbstractMappingParsingProcessor {

    protected IntermediaryMappingFileExtractor() {
        super(
                (releaseName) -> Lists.newArrayList(Paths.get(Constants.INTERMEDIARY_WORKING_DIR, "mappings", "mappings.tiny")),
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
                            null
                    );
                },
                (classes, line, releaseName) -> {
                    if (!line.startsWith("METHOD"))
                        return null;

                    final String[] components = line.split("\t");

                    final String parentClassOut = classes.get(components[1]).getOutput();

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
                            null);
                },
                (classes, line, releaseName) -> {
                    if (!line.startsWith("FIELD"))
                        return null;

                    final String[] components = line.split("\t");

                    final String parentClassOut = classes.get(components[1]).getOutput();

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
                            null
                    );
                },
                IParameterParser.NOOP,
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
