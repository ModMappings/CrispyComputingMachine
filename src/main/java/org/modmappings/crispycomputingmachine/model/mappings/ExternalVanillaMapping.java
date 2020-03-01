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
    private String parentMethodDescriptor;

    private ExternalVisibility visibility;
    private boolean isStatic;
    private String type;
    private String descriptor;

    private List<String> superClasses;

    public ExternalVanillaMapping(final String input, final String output, final ExternalMappableType mappableType, final String gameVersion, final Date gameVersionReleaseDate, final String parentClassMapping, final String parentMethodMapping, final String parentMethodDescriptor, final ExternalVisibility visibility, final boolean isStatic, final String type, final String descriptor, final List<String> superClasses) {
        this.input = input;
        this.output = output;
        this.mappableType = mappableType;
        this.gameVersion = gameVersion;
        this.gameVersionReleaseDate = gameVersionReleaseDate;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.parentMethodDescriptor = parentMethodDescriptor;
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

    public String getParentMethodDescriptor() {
        return parentMethodDescriptor;
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

        final ExternalVanillaMapping mapping = (ExternalVanillaMapping) o;

        if (isStatic() != mapping.isStatic()) return false;
        if (getInput() != null ? !getInput().equals(mapping.getInput()) : mapping.getInput() != null) return false;
        if (getOutput() != null ? !getOutput().equals(mapping.getOutput()) : mapping.getOutput() != null) return false;
        if (getMappableType() != mapping.getMappableType()) return false;
        if (getGameVersion() != null ? !getGameVersion().equals(mapping.getGameVersion()) : mapping.getGameVersion() != null)
            return false;
        if (getGameVersionReleaseDate() != null ? !getGameVersionReleaseDate().equals(mapping.getGameVersionReleaseDate()) : mapping.getGameVersionReleaseDate() != null)
            return false;
        if (getParentClassMapping() != null ? !getParentClassMapping().equals(mapping.getParentClassMapping()) : mapping.getParentClassMapping() != null)
            return false;
        if (getParentMethodMapping() != null ? !getParentMethodMapping().equals(mapping.getParentMethodMapping()) : mapping.getParentMethodMapping() != null)
            return false;
        if (getParentMethodDescriptor() != null ? !getParentMethodDescriptor().equals(mapping.getParentMethodDescriptor()) : mapping.getParentMethodDescriptor() != null)
            return false;
        if (getVisibility() != mapping.getVisibility()) return false;
        if (getType() != null ? !getType().equals(mapping.getType()) : mapping.getType() != null) return false;
        if (getDescriptor() != null ? !getDescriptor().equals(mapping.getDescriptor()) : mapping.getDescriptor() != null)
            return false;
        return getSuperClasses() != null ? getSuperClasses().equals(mapping.getSuperClasses()) : mapping.getSuperClasses() == null;
    }

    @Override
    public int hashCode() {
        int result = getInput() != null ? getInput().hashCode() : 0;
        result = 31 * result + (getOutput() != null ? getOutput().hashCode() : 0);
        result = 31 * result + (getMappableType() != null ? getMappableType().hashCode() : 0);
        result = 31 * result + (getGameVersion() != null ? getGameVersion().hashCode() : 0);
        result = 31 * result + (getGameVersionReleaseDate() != null ? getGameVersionReleaseDate().hashCode() : 0);
        result = 31 * result + (getParentClassMapping() != null ? getParentClassMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodMapping() != null ? getParentMethodMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodDescriptor() != null ? getParentMethodDescriptor().hashCode() : 0);
        result = 31 * result + (getVisibility() != null ? getVisibility().hashCode() : 0);
        result = 31 * result + (isStatic() ? 1 : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getDescriptor() != null ? getDescriptor().hashCode() : 0);
        result = 31 * result + (getSuperClasses() != null ? getSuperClasses().hashCode() : 0);
        return result;
    }
}
