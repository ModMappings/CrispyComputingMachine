package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISimpleFieldParser {

    boolean acceptsFile(@NotNull Path path);

    @NotNull
    Collection<ExternalMapping> parse(@NotNull final Set<ExternalMapping> classes, @NotNull final String line, final String releaseName);

    ISimpleFieldParser NOOP = new ISimpleFieldParser() {
        @Override
        public boolean acceptsFile(@NotNull final Path path)
        {
            return false;
        }

        @Override
        public @NotNull Collection<ExternalMapping> parse(
          final @NotNull Set<ExternalMapping> classes, final @NotNull String line, final String releaseName)
        {
            return List.of();
        }
    };
}
