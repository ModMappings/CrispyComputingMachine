package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface IContextualParameterParser {

    @Nullable
    ExternalMapping parse(@NotNull final ExternalMapping method, @NotNull final String line, final String releaseName);

    IContextualParameterParser NOOP = new IContextualParameterParser() {
        @Nullable
        @Override
        public ExternalMapping parse(@NotNull final ExternalMapping method, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
