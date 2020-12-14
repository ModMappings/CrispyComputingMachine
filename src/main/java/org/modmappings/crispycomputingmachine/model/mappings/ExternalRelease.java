package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.Date;
import java.util.LinkedList;

public class ExternalRelease {

    private final String name;
    private final Date releasedOn;
    private final LinkedList<ExternalClass> classes;
    private final boolean isPreRelease;
    private final boolean isSnapshot;

    public ExternalRelease(final String name, final Date releasedOn, final LinkedList<ExternalClass> classes, final boolean isPreRelease, final boolean isSnapshot) {
        this.name = name;
        this.releasedOn = releasedOn;
        this.classes = classes;
        this.isPreRelease = isPreRelease;
        this.isSnapshot = isSnapshot;
    }

    public String getName() {
        return name;
    }

    public Date getReleasedOn() {
        return releasedOn;
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
