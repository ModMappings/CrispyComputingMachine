package org.modmappings.crispycomputingmachine.processors.version;

import net.minecraftforge.srgutils.MinecraftVersion;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ConfigurationBasedMinecraftVersionFilter implements ItemProcessor<VersionsItem, VersionsItem> {

    @Value("${importer.versions:}")
    String[] versionsToImport;

    @Override
    public VersionsItem process(final VersionsItem item) throws Exception {
        if (versionsToImport == null || versionsToImport.length == 0)
            return item;

        return Arrays.stream(versionsToImport).anyMatch(v -> v.equals(item.getId())) ? item : null;
    }
}
