package org.modmappings.crispycomputingmachine.model.mappings;

public class ExternalField {

    private final String input;
    private final String output;
    private final String type;
    private final ExternalVisibility visibility;
    private final boolean isStatic;


    public ExternalField(final String input, final String output, final String type, final ExternalVisibility visibility, final boolean isStatic) {
        this.input = input;
        this.output = output;
        this.type = type;
        this.visibility = visibility;
        this.isStatic = isStatic;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public String getType() {
        return type;
    }

    public ExternalVisibility getVisibility() {
        return visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
