package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import java.util.List;

@FunctionalInterface
public interface IContextualParsingPostProcessor {

    void processFile(@NotNull final List<ExternalMapping> classes, @NotNull final List<ExternalMapping> methods, @NotNull final List<ExternalMapping> fields, @NotNull final List<ExternalMapping> parameters);

    IContextualParsingPostProcessor NOOP = new IContextualParsingPostProcessor() {
        @Override
        public void processFile(final @NotNull List<ExternalMapping> classes, final @NotNull List<ExternalMapping> methods, final @NotNull List<ExternalMapping> fields, final @NotNull List<ExternalMapping> parameters) {
            //NOOP Deliberate.
        }
    };
}
