package org.modmappings.crispycomputingmachine.processors.base.parsing;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;

@FunctionalInterface
public interface IClassParser {

    @Nullable
    ExternalMapping parse(@NotNull final String line, final String releaseName);

    IClassParser NOOP = new IClassParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
