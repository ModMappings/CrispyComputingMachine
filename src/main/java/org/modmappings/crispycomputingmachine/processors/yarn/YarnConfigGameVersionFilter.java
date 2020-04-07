package org.modmappings.crispycomputingmachine.processors.yarn;

import org.modmappings.crispycomputingmachine.processors.base.AbstractConfigGameVersionFilter;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class YarnConfigGameVersionFilter extends AbstractConfigGameVersionFilter {

    public YarnConfigGameVersionFilter() {
        super(releaseName -> releaseName.split("\\+")[0]);
    }
}
