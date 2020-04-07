package org.modmappings.crispycomputingmachine.processors.yarn;

import org.modmappings.crispycomputingmachine.processors.base.AbstractDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class YarnDownloadingProcessor extends AbstractDownloadingProcessor {

    public YarnDownloadingProcessor() {
        super(release -> {
                    try {
                        return new URL(Constants.YARN_MAPPING_REPO + release + "/yarn-" + release + "-v2.jar");
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(release, e);
                    }
                },
                Constants.YARN_JAR,
                Constants.YARN_MAPPING_NAME
        );
    }
}
