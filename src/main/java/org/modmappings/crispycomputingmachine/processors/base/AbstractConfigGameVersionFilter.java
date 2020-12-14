package org.modmappings.crispycomputingmachine.processors.base;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Function;

public abstract class AbstractConfigGameVersionFilter implements ItemProcessor<String, String> {

    private final Function<String, String> gameVersionNameExtractor;

    @Value("${importer.game_versions:}")
    String[] versionsToImport;

    public AbstractConfigGameVersionFilter(final Function<String, String> gameVersionNameExtractor) {
        this.gameVersionNameExtractor = gameVersionNameExtractor;
    }

    @Override
    public String process(@NotNull final String item) {
        if (versionsToImport == null || versionsToImport.length == 0)
            return item;

        final String gameVersionName = gameVersionNameExtractor.apply(item);
        return Arrays.asList(versionsToImport).contains(gameVersionName) ? item : null;
    }
}
