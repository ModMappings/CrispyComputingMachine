package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface IClassesPostProcessor
{
    void apply(@NotNull final String releaseName, @NotNull final Set<ExternalMapping> classes);

    IClassesPostProcessor NOOP = (releaseName, classes) -> {}; //Noop
}
