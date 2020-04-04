package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface ISimpleParameterParser {

    @Nullable
    ExternalMapping parse(@NotNull final Map<String, ExternalMapping> methods, @NotNull final String line, final String releaseName);

    ISimpleParameterParser NOOP = new ISimpleParameterParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull Map<String, ExternalMapping> methods, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
