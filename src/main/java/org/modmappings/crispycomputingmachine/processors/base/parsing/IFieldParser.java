package org.modmappings.crispycomputingmachine.processors.base.parsing;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface IFieldParser {

    @Nullable
    ExternalMapping parse(@NotNull final Map<String, ExternalMapping> classes, @NotNull final String line, final String releaseName);

    IFieldParser NOOP = new IFieldParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull Map<String, ExternalMapping> classes, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
