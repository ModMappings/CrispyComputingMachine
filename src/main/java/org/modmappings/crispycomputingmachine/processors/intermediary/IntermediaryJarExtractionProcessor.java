package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.crispycomputingmachine.processors.base.AbstractZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class IntermediaryJarExtractionProcessor extends AbstractZipExtractionProcessor {

    protected IntermediaryJarExtractionProcessor() {
        super(
                Constants.INTERMEDIARY_JAR,
                Constants.INTERMEDIARY_WORKING_DIR,
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
