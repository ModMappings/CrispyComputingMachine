package org.modmappings.crispycomputingmachine.processors.base.parsing.contextual;

import java.io.File;

@FunctionalInterface
public interface IContextualParsingPreProcessor {

    void processFile(File fileToBeProcessed);

    IContextualParsingPreProcessor NOOP = fileToBeProcessed -> {
        //NOOP
    };
}
