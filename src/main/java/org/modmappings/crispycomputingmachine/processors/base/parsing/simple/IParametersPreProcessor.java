package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import java.util.Set;

@FunctionalInterface
public interface IParametersPreProcessor
{
    void apply(@NotNull final String releaseName, @NotNull final Set<ExternalMapping> classes, @NotNull final Set<ExternalMapping> methods);

    IParametersPreProcessor NOOP = (releaseName, classes, methods) -> {}; //Noop
}
