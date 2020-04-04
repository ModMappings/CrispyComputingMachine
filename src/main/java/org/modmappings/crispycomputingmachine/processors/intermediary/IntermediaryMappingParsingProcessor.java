package org.modmappings.crispycomputingmachine.processors.intermediary;

import com.google.common.collect.Lists;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParameterParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.AbstractSimpleMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.ISimpleParameterParser;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class IntermediaryMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    protected IntermediaryMappingParsingProcessor() {
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
                            null);
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
                            null
                    );
                },
                IContextualParameterParser.NOOP,
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
