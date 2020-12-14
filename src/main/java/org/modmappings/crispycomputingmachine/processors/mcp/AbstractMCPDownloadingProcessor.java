package org.modmappings.crispycomputingmachine.processors.mcp;

import org.modmappings.crispycomputingmachine.processors.base.AbstractDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractMCPDownloadingProcessor extends AbstractDownloadingProcessor {

    public AbstractMCPDownloadingProcessor(
      final String MCP_REPO,
      final String MCP_ARTIFACT,
      final String MCP_FILENAME
    ) {
        super(release -> {
                    try {
                        return new URL(MCP_REPO + release + "/" + MCP_ARTIFACT + "-" + release + ".zip");
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(release, e);
                    }
                },
                MCP_FILENAME,
                Constants.MCP_MAPPING_NAME
        );
    }
}
