package org.modmappings.crispycomputingmachine.processors.official;

import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ExistingOfficialMappingMinecraftVersionFilter implements ItemProcessor<VersionsItem, VersionsItem> {

    private final GameVersionRepository gameVersionRepository;

    public ExistingOfficialMappingMinecraftVersionFilter(final GameVersionRepository gameVersionRepository) {
        this.gameVersionRepository = gameVersionRepository;
    }

    @Override
    public VersionsItem process(final VersionsItem item) throws Exception {
        return gameVersionRepository.findAllBy("\\A" + item.getId().replace(".", "\\.") + "\\Z", null, null, Pageable.unpaged())
                .flatMapIterable(Function.identity())
                .next()
                .hasElement()
                .block() ? null : item;
    }
}
