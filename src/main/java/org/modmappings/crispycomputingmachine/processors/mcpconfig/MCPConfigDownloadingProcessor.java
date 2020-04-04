package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.modmappings.crispycomputingmachine.processors.base.AbstractDownloadingProcessor;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class MCPConfigDownloadingProcessor extends AbstractDownloadingProcessor {

    public MCPConfigDownloadingProcessor() {
        super(
                releaseName -> {
                    try {
                        return new URL(String.format("%s%s/mcp_config-%s.zip", Constants.MCP_CONFIG_MAPPING_REPO, releaseName, releaseName));
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(releaseName, e);
                    }
                },
                Constants.MCP_CONFIG_ZIP,
                Constants.MCP_CONFIG_MAPPING_NAME
        );
    }
}
