package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.crispycomputingmachine.processors.base.AbstractDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class IntermediaryDownloadingProcessor extends AbstractDownloadingProcessor {

    public IntermediaryDownloadingProcessor() {
        super(release -> {
                    try {
                        return new URL(Constants.INTERMEDIARY_MAPPING_REPO + release + "/intermediary-" + release + ".jar");
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(release);
                    }
                },
                Constants.INTERMEDIARY_JAR,
                Constants.INTERMEDIARY_MAPPING_NAME
        );
    }
}
