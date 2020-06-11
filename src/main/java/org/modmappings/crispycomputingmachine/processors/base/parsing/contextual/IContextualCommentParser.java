package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IContextualCommentParser {

    String parse(@NotNull final String line);

    IContextualCommentParser NOOP = new IContextualCommentParser() {
        @Override
        public String parse(final @NotNull String line) {
            return null;
        }
    };
}
