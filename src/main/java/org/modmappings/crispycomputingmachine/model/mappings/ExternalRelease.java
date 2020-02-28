package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.LinkedList;
import java.util.Set;

public class ExternalRelease {

    private final String name;
    private final LinkedList<ExternalClass> classes;
    private final boolean isPreRelease;
    private final boolean isSnapshot;

    public ExternalRelease(final String name, final LinkedList<ExternalClass> classes, final boolean isPreRelease, final boolean isSnapshot) {
        this.name = name;
        this.classes = classes;
        this.isPreRelease = isPreRelease;
        this.isSnapshot = isSnapshot;
    }

    public String getName() {
        return name;
    }

    public LinkedList<ExternalClass> getClasses() {
        return classes;
    }

    public boolean isPreRelease() {
        return isPreRelease;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }
}
