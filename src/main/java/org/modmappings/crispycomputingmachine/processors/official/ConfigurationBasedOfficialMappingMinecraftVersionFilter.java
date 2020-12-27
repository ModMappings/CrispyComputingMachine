package org.modmappings.crispycomputingmachine.processors.official;

import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ConfigurationBasedOfficialMappingMinecraftVersionFilter implements ItemProcessor<VersionsItem, VersionsItem> {

    @Value("${importer.game_versions:}")
    String[] versionsToImport;

    @Override
    public VersionsItem process(final VersionsItem item) throws Exception {
        if (versionsToImport == null || versionsToImport.length == 0)
            return item;

        return Arrays.stream(versionsToImport).anyMatch(v -> v.equals(item.getId())) ? item : null;
    }
}
