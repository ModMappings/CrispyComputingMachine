package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.Objects;

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
    private boolean isLocked = false;

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

    public boolean isLocked()
    {
        return isLocked;
    }

    public void setLocked(final boolean locked)
    {
        isLocked = locked;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ExternalMapping))
        {
            return false;
        }
        final ExternalMapping that = (ExternalMapping) o;
        return isStatic() == that.isStatic() && isLocked() == that.isLocked() && Objects.equals(getParentMethodMapping(), that.getParentMethodMapping())
                 && Objects.equals(getParentMethodDescriptor(), that.getParentMethodDescriptor()) && Objects.equals(getInput(), that.getInput())
                 && Objects.equals(getOutput(), that.getOutput()) && getMappableType() == that.getMappableType() && Objects.equals(getGameVersion(),
          that.getGameVersion()) && Objects.equals(getReleaseName(), that.getReleaseName()) && Objects.equals(getParentClassMapping(), that.getParentClassMapping())
                 && Objects.equals(getType(), that.getType()) && Objects.equals(getDescriptor(), that.getDescriptor()) && Objects.equals(getSignature(),
          that.getSignature()) && Objects.equals(getIndex(), that.getIndex()) && Objects.equals(getDocumentation(), that.getDocumentation())
                 && getExternalDistribution() == that.getExternalDistribution();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getParentMethodMapping(),
          getParentMethodDescriptor(),
          getInput(),
          getOutput(),
          getMappableType(),
          getGameVersion(),
          getReleaseName(),
          getParentClassMapping(),
          getType(),
          getDescriptor(),
          getSignature(),
          getIndex(),
          isStatic(),
          getDocumentation(),
          getExternalDistribution(),
          isLocked());
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
