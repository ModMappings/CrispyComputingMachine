package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface ISimpleFieldParser {

    @Nullable
    ExternalMapping parse(@NotNull final Map<String, ExternalMapping> classes, @NotNull final String line, final String releaseName);

    ISimpleFieldParser NOOP = new ISimpleFieldParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull Map<String, ExternalMapping> classes, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
