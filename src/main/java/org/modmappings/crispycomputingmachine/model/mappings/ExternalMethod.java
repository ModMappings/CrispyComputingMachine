package org.modmappings.crispycomputingmachine.model.mappings;

import org.modmappings.crispycomputingmachine.utils.MethodRef;

import java.util.Set;

public class ExternalMethod {

    private final String input;
    private final String output;

    private final ExternalVisibility externalVisibility;
    private final boolean isStatic;

    private final String descriptor;
    private final String signature;
    private final boolean isExternal;

    private final Set<MethodRef> overrides;

    public ExternalMethod(final String input, final String output, final ExternalVisibility externalVisibility, final boolean isStatic, final String descriptor, final String signature, final boolean isExternal, final Set<MethodRef> overrides) {
        this.input = input;
        this.output = output;
        this.externalVisibility = externalVisibility;
        this.isStatic = isStatic;
        this.descriptor = descriptor;
        this.signature = signature;
        this.isExternal = isExternal;
        this.overrides = overrides;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public ExternalVisibility getVisibility() {
        return externalVisibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public Set<MethodRef> getOverrides() {
        return overrides;
    }

}
