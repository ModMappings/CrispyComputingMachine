package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.crispycomputingmachine.processors.base.AbstractConfigGameVersionFilter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Function;

@Component
public class IntermediaryConfigGameVersionFilter extends AbstractConfigGameVersionFilter {

    public IntermediaryConfigGameVersionFilter() {
        super(Function.identity());
    }
}
