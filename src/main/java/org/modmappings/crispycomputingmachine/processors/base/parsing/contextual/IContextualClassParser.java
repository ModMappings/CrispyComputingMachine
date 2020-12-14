package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;

@FunctionalInterface
public interface IContextualClassParser {

    @Nullable
    ExternalMapping parse(@NotNull final String line, final String releaseName);

    IContextualClassParser NOOP = new IContextualClassParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
