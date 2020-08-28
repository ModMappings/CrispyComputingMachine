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
    private final Integer index;
    private boolean isStatic;
    private String documentation = "";
    private ExternalDistribution externalDistribution = ExternalDistribution.UNKNOWN;

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
                           final String signature,
                           final Integer index,
                           final boolean isStatic) {
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
        this.index = index;
        this.isStatic = isStatic;
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

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(final String documentation) {
        this.documentation = documentation;
    }

    public Integer getIndex() {
        return index;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(final boolean aStatic) {
        isStatic = aStatic;
    }

    public ExternalDistribution getExternalDistribution()
    {
        return externalDistribution;
    }

    public void setExternalDistribution(final ExternalDistribution externalDistribution)
    {
        this.externalDistribution = externalDistribution;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final ExternalMapping mapping = (ExternalMapping) o;

        if (isStatic() != mapping.isStatic())
        {
            return false;
        }
        if (getParentMethodMapping() != null ? !getParentMethodMapping().equals(mapping.getParentMethodMapping()) : mapping.getParentMethodMapping() != null)
        {
            return false;
        }
        if (getParentMethodDescriptor() != null ? !getParentMethodDescriptor().equals(mapping.getParentMethodDescriptor()) : mapping.getParentMethodDescriptor() != null)
        {
            return false;
        }
        if (getInput() != null ? !getInput().equals(mapping.getInput()) : mapping.getInput() != null)
        {
            return false;
        }
        if (getOutput() != null ? !getOutput().equals(mapping.getOutput()) : mapping.getOutput() != null)
        {
            return false;
        }
        if (getMappableType() != mapping.getMappableType())
        {
            return false;
        }
        if (getGameVersion() != null ? !getGameVersion().equals(mapping.getGameVersion()) : mapping.getGameVersion() != null)
        {
            return false;
        }
        if (getReleaseName() != null ? !getReleaseName().equals(mapping.getReleaseName()) : mapping.getReleaseName() != null)
        {
            return false;
        }
        if (getParentClassMapping() != null ? !getParentClassMapping().equals(mapping.getParentClassMapping()) : mapping.getParentClassMapping() != null)
        {
            return false;
        }
        if (getType() != null ? !getType().equals(mapping.getType()) : mapping.getType() != null)
        {
            return false;
        }
        if (getDescriptor() != null ? !getDescriptor().equals(mapping.getDescriptor()) : mapping.getDescriptor() != null)
        {
            return false;
        }
        if (getSignature() != null ? !getSignature().equals(mapping.getSignature()) : mapping.getSignature() != null)
        {
            return false;
        }
        if (getIndex() != null ? !getIndex().equals(mapping.getIndex()) : mapping.getIndex() != null)
        {
            return false;
        }
        if (getDocumentation() != null ? !getDocumentation().equals(mapping.getDocumentation()) : mapping.getDocumentation() != null)
        {
            return false;
        }
        return externalDistribution == mapping.externalDistribution;
    }

    @Override
    public int hashCode()
    {
        int result = getParentMethodMapping() != null ? getParentMethodMapping().hashCode() : 0;
        result = 31 * result + (getParentMethodDescriptor() != null ? getParentMethodDescriptor().hashCode() : 0);
        result = 31 * result + (getInput() != null ? getInput().hashCode() : 0);
        result = 31 * result + (getOutput() != null ? getOutput().hashCode() : 0);
        result = 31 * result + (getMappableType() != null ? getMappableType().hashCode() : 0);
        result = 31 * result + (getGameVersion() != null ? getGameVersion().hashCode() : 0);
        result = 31 * result + (getReleaseName() != null ? getReleaseName().hashCode() : 0);
        result = 31 * result + (getParentClassMapping() != null ? getParentClassMapping().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getDescriptor() != null ? getDescriptor().hashCode() : 0);
        result = 31 * result + (getSignature() != null ? getSignature().hashCode() : 0);
        result = 31 * result + (getIndex() != null ? getIndex().hashCode() : 0);
        result = 31 * result + (isStatic() ? 1 : 0);
        result = 31 * result + (getDocumentation() != null ? getDocumentation().hashCode() : 0);
        result = 31 * result + (externalDistribution != null ? externalDistribution.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "ExternalMapping{" +
                 "parentMethodMapping='" + parentMethodMapping + '\'' +
                 ", parentMethodDescriptor='" + parentMethodDescriptor + '\'' +
                 ", input='" + input + '\'' +
                 ", output='" + output + '\'' +
                 ", mappableType=" + mappableType +
                 ", gameVersion='" + gameVersion + '\'' +
                 ", releaseName='" + releaseName + '\'' +
                 ", parentClassMapping='" + parentClassMapping + '\'' +
                 ", type='" + type + '\'' +
                 ", descriptor='" + descriptor + '\'' +
                 ", signature='" + signature + '\'' +
                 ", index=" + index +
                 ", isStatic=" + isStatic +
                 ", documentation='" + documentation + '\'' +
                 ", externalDistribution=" + externalDistribution +
                 '}';
    }
}
