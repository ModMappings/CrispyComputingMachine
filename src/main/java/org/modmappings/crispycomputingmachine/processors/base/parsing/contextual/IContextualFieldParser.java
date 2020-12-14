package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface IContextualFieldParser {

    @Nullable
    ExternalMapping parse(@NotNull final ExternalMapping clazz, @NotNull final String line, final String releaseName);

    IContextualFieldParser NOOP = new IContextualFieldParser() {
        @Nullable
        @Override
        public ExternalMapping parse(@NotNull final ExternalMapping clazz, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
