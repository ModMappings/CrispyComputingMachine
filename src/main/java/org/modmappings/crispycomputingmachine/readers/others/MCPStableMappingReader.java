package org.modmappings.crispycomputingmachine.readers.others;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MCPStableMappingReader extends AbstractMavenBasedMappingReader
{
    public MCPStableMappingReader(
      final CompositeItemProcessor<String, List<ExternalMapping>> internalMCPStableMappingReaderProcessor
    )
    {
        super(internalMCPStableMappingReaderProcessor, Constants.MCP_MAPPING_NAME, Constants.MCP_STABLE_MAVEN_METADATA_FILE);
    }
}
