package org.modmappings.crispycomputingmachine.readers.mappings;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MCPConfigMappingReader extends AbstractMavenBasedMappingReader {

    public MCPConfigMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> internalMCPConfigMappingReaderProcessor) {
        super(internalMCPConfigMappingReaderProcessor, Constants.MCP_CONFIG_MAPPING_NAME, Constants.MCP_CONFIG_MAVEN_METADATA_FILE);
    }

}
