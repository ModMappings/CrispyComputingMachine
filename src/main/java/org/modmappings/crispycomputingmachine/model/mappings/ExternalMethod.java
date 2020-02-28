package org.modmappings.crispycomputingmachine.model.mappings;

public class ExternalMethod {

    private final String input;
    private final String output;

    private final ExternalVisibility externalVisibility;
    private final boolean isStatic;

    private final String signature;

    public ExternalMethod(final String input, final String output, final ExternalVisibility externalVisibility, final boolean isStatic, final String signature) {
        this.input = input;
        this.output = output;
        this.externalVisibility = externalVisibility;
        this.isStatic = isStatic;
        this.signature = signature;
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

    public String getSignature() {
        return signature;
    }
}
