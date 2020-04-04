package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;

@FunctionalInterface
public interface ISimpleClassParser {

    @Nullable
    ExternalMapping parse(@NotNull final String line, final String releaseName);

    ISimpleClassParser NOOP = new ISimpleClassParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
