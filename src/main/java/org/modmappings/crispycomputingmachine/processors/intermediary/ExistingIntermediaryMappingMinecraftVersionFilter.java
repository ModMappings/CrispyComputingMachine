package org.modmappings.crispycomputingmachine.processors.intermediary;

import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ExistingIntermediaryMappingMinecraftVersionFilter implements ItemProcessor<String, String> {

    private final GameVersionRepository gameVersionRepository;

    public ExistingIntermediaryMappingMinecraftVersionFilter(final GameVersionRepository gameVersionRepository) {
        this.gameVersionRepository = gameVersionRepository;
    }

    @Override
    public String process(final String item) throws Exception {
        return gameVersionRepository.findAllBy("\\A" + item.replace(".", "\\.") + "\\Z", null, null, Pageable.unpaged())
                .flatMapIterable(Function.identity())
                .next()
                .hasElement()
                .block() ? item : null;
    }
}
