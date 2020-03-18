package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ConfigurationIntermediaryMappingMinecraftVersionFilter implements ItemProcessor<String, String> {

    @Value("${importer.versions:}")
    String[] versionsToImport;

    @Override
    public String process(final String item) throws Exception {
        if (versionsToImport == null || versionsToImport.length == 0)
            return item;

        return Arrays.stream(versionsToImport).anyMatch(v -> v.equals(item)) ? item : null;
    }
}
