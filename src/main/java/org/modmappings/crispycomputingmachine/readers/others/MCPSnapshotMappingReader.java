package org.modmappings.crispycomputingmachine.readers.others;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MCPSnapshotMappingReader extends AbstractMavenBasedMappingReader
{
    public MCPSnapshotMappingReader(
      final CompositeItemProcessor<String, List<ExternalMapping>> internalMCPSnapshotMappingReaderProcessor
    )
    {
        super(internalMCPSnapshotMappingReaderProcessor, Constants.MCP_MAPPING_NAME, Constants.MCP_SNAPSHOT_MAVEN_METADATA_FILE);
    }
}
