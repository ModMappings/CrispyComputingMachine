package org.modmappings.crispycomputingmachine.processors;

import net.minecraftforge.srgutils.MinecraftVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

public class ExistingMinecraftVersionFilter implements ItemProcessor<MinecraftVersion, MinecraftVersion> {

    private static final Logger LOGGER = LogManager.getLogger(ExistingMinecraftVersionFilter.class);

    private final GameVersionRepository gameVersionRepository;

    public ExistingMinecraftVersionFilter(final GameVersionRepository gameVersionRepository) {
        this.gameVersionRepository = gameVersionRepository;
    }

    @Override
    public MinecraftVersion process(final MinecraftVersion item) throws Exception {
        LOGGER.info("Checking if: " + item.toString() + " has alread been imported.");
        final MinecraftVersion ret = gameVersionRepository.findAllBy("\\A" + item.toString().replace(".", "\\.") + "\\Z", null, null, Pageable.unpaged())
                .flatMapIterable(Function.identity())
                .next()
                .hasElement()
                .block() ? null : item;

        if (ret != null)
        {
            LOGGER.info("Version: " + ret.toString() + " is new. Importing...");
        }
        else
        {
            LOGGER.info("Version: " + item.toString() + " already exists. Skipping...");
        }

        return ret;
    }
}
