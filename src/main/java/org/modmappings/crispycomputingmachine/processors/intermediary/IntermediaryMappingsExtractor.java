package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.crispycomputingmachine.processors.base.AbstractZipExtractor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class IntermediaryMappingsExtractor extends AbstractZipExtractor {

    protected IntermediaryMappingsExtractor() {
        super(
                Constants.INTERMEDIARY_JAR,
                Constants.INTERMEDIARY_WORKING_DIR,
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
