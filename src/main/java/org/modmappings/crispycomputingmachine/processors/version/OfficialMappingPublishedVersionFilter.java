package org.modmappings.crispycomputingmachine.processors.version;

import net.minecraftforge.srgutils.MinecraftVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class OfficialMappingPublishedVersionFilter implements ItemProcessor<VersionsItem, VersionsItem> {

    private static final Logger LOGGER = LogManager.getLogger(OfficialMappingPublishedVersionFilter.class);

    private static final Date INITIAL_PUBLISH_DATE_OF_MAPPING = Date.from(Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2019-09-04T11:19:34+00:00")));

    @Override
    public VersionsItem process(final VersionsItem item) throws Exception {
        LOGGER.info("Checking if: " + item.getId() + " has a official mapping.");

        final Date date = Date.from(Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(item.getReleaseTime())));
        final boolean hasMappings = date.compareTo(INITIAL_PUBLISH_DATE_OF_MAPPING) >= 0;

        if (!hasMappings)
        {
            LOGGER.info("Skipping: " + item.toString() + ". No official mappings available.");
            return null;
        }
        else
        {
            LOGGER.info("Potentially mappings are available for: " + item.toString());
            return item;
        }
    }
}
