package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.Set;

public class ExternalClass {
    
    private final String input;
    private final String output;

    private final Set<ExternalClass> superClasses;
    private final Set<ExternalMethod> methods;
    private final Set<ExternalField> fields;

    private ExternalVisibility visibility;
    private boolean isStatic;
    private boolean isAbstract;
    private boolean isInterface;
    private boolean isEnum;
    private boolean isExternal;

    public ExternalClass(final String input, final String output, final Set<ExternalClass> superClasses, final Set<ExternalMethod> methods, final Set<ExternalField> fields, final ExternalVisibility visibility, final boolean isStatic, final boolean isAbstract, final boolean isInterface, final boolean isEnum, final boolean isExternal) {
        this.input = input;
        this.output = output;
        this.superClasses = superClasses;
        this.methods = methods;
        this.fields = fields;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.isAbstract = isAbstract;
        this.isInterface = isInterface;
        this.isEnum = isEnum;
        this.isExternal = isExternal;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public Set<ExternalClass> getSuperClasses() {
        return superClasses;
    }

    public Set<ExternalMethod> getMethods() {
        return methods;
    }

    public Set<ExternalField> getFields() {
        return fields;
    }

    public ExternalVisibility getVisibility() {
        return visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isExternal() { return isExternal; }
}
