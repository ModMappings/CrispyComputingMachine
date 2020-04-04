package org.modmappings.crispycomputingmachine.readers.mappings;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IntermediaryMappingReader extends AbstractMavenBasedMappingReader
{
    public IntermediaryMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> internalIntermediaryMappingReaderProcessor) {
        super(internalIntermediaryMappingReaderProcessor, Constants.INTERMEDIARY_MAPPING_NAME, Constants.INTERMEDIARY_MAVEN_METADATA_FILE);
    }


}
