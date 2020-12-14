package org.modmappings.crispycomputingmachine.processors.yarn;

import org.modmappings.crispycomputingmachine.processors.base.AbstractZipExtractionProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class YarnJarExtractionProcessor extends AbstractZipExtractionProcessor {

    protected YarnJarExtractionProcessor() {
        super(
                Constants.YARN_JAR,
                Constants.YARN_WORKING_DIR,
                Constants.YARN_MAPPING_NAME
        );
    }
}
