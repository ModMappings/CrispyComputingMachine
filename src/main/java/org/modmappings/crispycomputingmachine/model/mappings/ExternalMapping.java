package org.modmappings.crispycomputingmachine.model.mappings;

public class ExternalMapping {
    private final String parentMethodMapping;
    private final String parentMethodDescriptor;
    private final String input;
    private final String output;
    private final ExternalMappableType mappableType;
    private final String gameVersion;
    private final String releaseName;
    private final String parentClassMapping;
    private final String type;
    private final String descriptor;
    private final String signature;

    public ExternalMapping(final String input,
                           final String output,
                           final ExternalMappableType mappableType,
                           final String gameVersion,
                           final String releaseName,
                           final String parentClassMapping,
                           final String parentMethodMapping,
                           final String parentMethodDescriptor,
                           final String type,
                           final String descriptor,
                           final String signature) {
        this.input = input;
        this.output = output;
        this.mappableType = mappableType;
        this.gameVersion = gameVersion;
        this.parentClassMapping = parentClassMapping;
        this.releaseName = releaseName;
        this.type = type;
        this.descriptor = descriptor;
        this.signature = signature;
        this.parentMethodMapping = parentMethodMapping;
        this.parentMethodDescriptor = parentMethodDescriptor;
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

    public String getReleaseName() {
        return releaseName;
    }

    public String getParentClassMapping() {
        return parentClassMapping;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public String getParentMethodMapping() {
        return parentMethodMapping;
    }

    public String getParentMethodDescriptor() {
        return parentMethodDescriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalMapping)) return false;

        final ExternalMapping that = (ExternalMapping) o;

        if (getParentMethodMapping() != null ? !getParentMethodMapping().equals(that.getParentMethodMapping()) : that.getParentMethodMapping() != null)
            return false;
        if (getParentMethodDescriptor() != null ? !getParentMethodDescriptor().equals(that.getParentMethodDescriptor()) : that.getParentMethodDescriptor() != null)
            return false;
        if (!getInput().equals(that.getInput())) return false;
        if (!getOutput().equals(that.getOutput())) return false;
        if (getMappableType() != that.getMappableType()) return false;
        if (!getGameVersion().equals(that.getGameVersion())) return false;
        if (!getReleaseName().equals(that.getReleaseName())) return false;
        if (getParentClassMapping() != null ? !getParentClassMapping().equals(that.getParentClassMapping()) : that.getParentClassMapping() != null)
            return false;
        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) return false;
        if (getDescriptor() != null ? !getDescriptor().equals(that.getDescriptor()) : that.getDescriptor() != null)
            return false;
        return getSignature() != null ? getSignature().equals(that.getSignature()) : that.getSignature() == null;
    }

    @Override
    public int hashCode() {
        int result = getParentMethodMapping() != null ? getParentMethodMapping().hashCode() : 0;
        result = 31 * result + (getParentMethodDescriptor() != null ? getParentMethodDescriptor().hashCode() : 0);
        result = 31 * result + getInput().hashCode();
        result = 31 * result + getOutput().hashCode();
        result = 31 * result + getMappableType().hashCode();
        result = 31 * result + getGameVersion().hashCode();
        result = 31 * result + getReleaseName().hashCode();
        result = 31 * result + (getParentClassMapping() != null ? getParentClassMapping().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getDescriptor() != null ? getDescriptor().hashCode() : 0);
        result = 31 * result + (getSignature() != null ? getSignature().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ExternalMapping{" +
                "input='" + input + '\'' +
                ", output='" + output + '\'' +
                '}';
    }
}
