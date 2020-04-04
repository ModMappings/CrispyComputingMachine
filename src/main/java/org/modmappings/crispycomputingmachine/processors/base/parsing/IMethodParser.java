package org.modmappings.crispycomputingmachine.processors.base.parsing;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;

import javax.annotation.Nullable;
import java.util.Map;

@FunctionalInterface
public interface IMethodParser {

    @Nullable
    ExternalMapping parse(@NotNull final Map<String, ExternalMapping> classes, @NotNull final String line, final String releaseName);

    IMethodParser NOOP = new IMethodParser() {
        @Nullable
        @Override
        public ExternalMapping parse(final @NotNull Map<String, ExternalMapping> classes, final @NotNull String line, final String releaseName) {
            return null;
        }
    };
}
