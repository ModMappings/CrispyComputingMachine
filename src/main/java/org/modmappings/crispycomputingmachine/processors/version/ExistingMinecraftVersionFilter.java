package org.modmappings.crispycomputingmachine.processors.version;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ExistingMinecraftVersionFilter implements ItemProcessor<VersionsItem, VersionsItem> {

    private static final Logger LOGGER = LogManager.getLogger(ExistingMinecraftVersionFilter.class);

    private final GameVersionRepository gameVersionRepository;

    public ExistingMinecraftVersionFilter(final GameVersionRepository gameVersionRepository) {
        this.gameVersionRepository = gameVersionRepository;
    }

    @Override
    public VersionsItem process(final VersionsItem item) throws Exception {
        LOGGER.info("Checking if: " + item.toString() + " has alread been imported.");
        final VersionsItem ret = gameVersionRepository.findAllBy("\\A" + item.getId().replace(".", "\\.") + "\\Z", null, null, Pageable.unpaged())
                .flatMapIterable(Function.identity())
                .next()
                .hasElement()
                .block() ? null : item;

        if (ret != null)
        {
            LOGGER.info("Version: " + ret.getId() + " is new. Importing...");
        }
        else
        {
            LOGGER.info("Version: " + item.getId() + " already exists. Skipping...");
        }

        return ret;
    }
}
