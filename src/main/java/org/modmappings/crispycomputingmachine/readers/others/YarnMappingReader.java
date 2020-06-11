package org.modmappings.crispycomputingmachine.readers.others;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class YarnMappingReader extends AbstractMavenBasedMappingReader
{
    public YarnMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> internalYarnMappingReaderProcessor) {
        super(internalYarnMappingReaderProcessor, Constants.YARN_MAPPING_NAME, Constants.YARN_MAVEN_METADATA_FILE);
    }
}
