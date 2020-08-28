package org.modmappings.crispycomputingmachine.processors.mcp;

import org.modmappings.crispycomputingmachine.processors.base.AbstractConfigGameVersionFilter;
import org.springframework.stereotype.Component;

@Component
public class MCPConfigGameVersionFilter extends AbstractConfigGameVersionFilter {

    public MCPConfigGameVersionFilter() {
        super(releaseName -> releaseName.split("-")[1]);
    }
}
