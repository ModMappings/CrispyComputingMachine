package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import java.util.Set;

@FunctionalInterface
public interface IMethodsPreProcessor
{
    void apply(@NotNull final String releaseName, @NotNull final Set<ExternalMapping> classes);

    IMethodsPreProcessor NOOP = (releaseName, classes) -> {}; //Noop
}
