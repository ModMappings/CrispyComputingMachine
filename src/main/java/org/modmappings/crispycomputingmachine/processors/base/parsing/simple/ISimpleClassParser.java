package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface ISimpleClassParser {

    boolean acceptsFile(@NotNull Path path);

    @NotNull
    Collection<ExternalMapping> parse(@NotNull final String line, final String releaseName);

    ISimpleClassParser NOOP = new ISimpleClassParser() {
        @Override
        public boolean acceptsFile(@NotNull final Path path)
        {
            return false;
        }

        @NotNull
        @Override
        public Collection<ExternalMapping> parse(final @NotNull String line, final String releaseName) {
            return List.of();
        }
    };
}
