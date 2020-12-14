package org.modmappings.crispycomputingmachine.processors.base.parsing.simple;

import org.jetbrains.annotations.NotNull;

public interface IClassesPreProcessor
{
    void apply(@NotNull final String releaseName);

    IClassesPreProcessor NOOP = (releaseName) -> {}; //Noop
}
