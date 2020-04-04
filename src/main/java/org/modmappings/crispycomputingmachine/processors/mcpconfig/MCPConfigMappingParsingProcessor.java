package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import com.google.common.collect.Lists;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.AbstractContextualMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.contextual.IContextualParameterParser;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.AbstractSimpleMappingParsingProcessor;
import org.modmappings.crispycomputingmachine.processors.base.parsing.simple.ISimpleParameterParser;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;

@Component
public class MCPConfigMappingParsingProcessor extends AbstractContextualMappingParsingProcessor {

    protected MCPConfigMappingParsingProcessor() {
        super(
                (releaseName) -> Lists.newArrayList(Paths.get(Constants.MCP_CONFIG_WORKING_DIR, "config", "joined.tsrg")),
                (line, releaseName) -> {
                    final String[] components = line.split(" ");
                    if (components.length != 2)
                        return null;

                    String parentClassOut = null;
                    if (components[1].contains("$"))
                        parentClassOut = components[1].substring(0, components[1].indexOf("$"));

                    return new ExternalMapping(
                            components[0],
                            components[1],
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
                    if (!line.startsWith("\t"))
                        return null;

                    final String workingLine = line.trim();
                    final String[] components = workingLine.split(" ");

                    if (components.length != 3)
                        return null;

                    final String parentClassOut = parentClass.getOutput();

                    return new ExternalMapping(
                            components[0],
                            components[2],
                            ExternalMappableType.METHOD,
                            releaseName,
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            null,
                            components[1],
                            null);
                },
                (parentClass, line, releaseName) -> {
                    if (!line.startsWith("\t"))
                        return null;

                    final String workingLine = line.trim();
                    final String[] components = workingLine.split(" ");

                    if (components.length != 2)
                        return null;

                    final String parentClassOut = parentClass.getOutput();


                    return new ExternalMapping(
                            components[0],
                            components[1],
                            ExternalMappableType.FIELD,
                            releaseName,
                            releaseName,
                            parentClassOut,
                            null,
                            null,
                            "*",
                            null,
                            null
                    );
                },
                IContextualParameterParser.NOOP,
                Constants.MCP_CONFIG_MAPPING_NAME
        );
    }
}
