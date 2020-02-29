package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ExternalVanillaMapping {

    private String input;
    private String output;
    private ExternalMappableType mappableType;

    private String gameVersion;
    private Date gameVersionReleaseDate;

    private String parentClassMapping;
    private String parentMethodMapping;

    private ExternalVisibility visibility;
    private boolean isStatic;
    private String type;
    private String descriptor;

    private List<String> superClasses;

    public ExternalVanillaMapping(final String input, final String output, final ExternalMappableType mappableType, final String gameVersion, final Date gameVersionReleaseDate, final String parentClassMapping, final String parentMethodMapping, final ExternalVisibility visibility, final boolean isStatic, final String type, final String descriptor, final List<String> superClasses) {
        this.input = input;
        this.output = output;
        this.mappableType = mappableType;
        this.gameVersion = gameVersion;
        this.gameVersionReleaseDate = gameVersionReleaseDate;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.type = type;
        this.descriptor = descriptor;
        this.superClasses = superClasses;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public ExternalMappableType getMappableType() {
        return mappableType;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public Date getGameVersionReleaseDate() {
        return gameVersionReleaseDate;
    }

    public String getParentClassMapping() {
        return parentClassMapping;
    }

    public String getParentMethodMapping() {
        return parentMethodMapping;
    }

    public ExternalVisibility getVisibility() {
        return visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public List<String> getSuperClasses() {
        return superClasses;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ExternalVanillaMapping that = (ExternalVanillaMapping) o;
        return isStatic() == that.isStatic() &&
                Objects.equals(getInput(), that.getInput()) &&
                Objects.equals(getOutput(), that.getOutput()) &&
                getMappableType() == that.getMappableType() &&
                Objects.equals(getGameVersion(), that.getGameVersion()) &&
                Objects.equals(getGameVersionReleaseDate(), that.getGameVersionReleaseDate()) &&
                Objects.equals(getParentClassMapping(), that.getParentClassMapping()) &&
                Objects.equals(getParentMethodMapping(), that.getParentMethodMapping()) &&
                getVisibility() == that.getVisibility() &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getDescriptor(), that.getDescriptor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInput(), getOutput(), getMappableType(), getGameVersion(), getGameVersionReleaseDate(), getParentClassMapping(), getParentMethodMapping(), getVisibility(), isStatic(), getType(), getDescriptor());
    }
}
