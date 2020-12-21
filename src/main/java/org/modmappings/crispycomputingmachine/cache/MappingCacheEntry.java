package org.modmappings.crispycomputingmachine.cache;

import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.UUID;

public class MappingCacheEntry {

    private String input;
    private String output;
    private UUID mappableId;
    private UUID versionedMappableId;
    private UUID mappingId;
    private MappableTypeDMO mappableType;
    private String parentClassOutput;
    private String parentMethodOutput;
    private String parentMethodDescriptor;
    private UUID gameVersionId;
    private String gameVersionName;
    private UUID releaseId;
    private String type;
    private String descriptor;
    private final String originalDescriptor;
    private final String documentation;
    private int index;
    private boolean isStatic;

    public MappingCacheEntry(
      final String input,
      final String output,
      final UUID mappableId,
      final UUID versionedMappableId,
      final UUID mappingId,
      final MappableTypeDMO mappableType,
      final String parentClassOutput,
      final String parentMethodOutput,
      final String parentMethodDescriptor,
      final UUID gameVersionId,
      final String gameVersionName,
      final UUID releaseId,
      final String type,
      final String descriptor,
      final String documentation,
      final int index,
      final boolean isStatic) {
        this.input = input;
        this.output = output;
        this.mappableId = mappableId;
        this.versionedMappableId = versionedMappableId;
        this.mappingId = mappingId;
        this.mappableType = mappableType;
        this.parentClassOutput = parentClassOutput;
        this.parentMethodOutput = parentMethodOutput;
        this.parentMethodDescriptor = parentMethodDescriptor;
        this.gameVersionId = gameVersionId;
        this.gameVersionName = gameVersionName;
        this.releaseId = releaseId;
        this.type = type;
        this.descriptor = descriptor;
        this.originalDescriptor = descriptor;
        this.documentation = documentation;
        this.index = index;
        this.isStatic = isStatic;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public UUID getMappableId() {
        return mappableId;
    }

    public UUID getVersionedMappableId() {
        return versionedMappableId;
    }

    public UUID getMappingId() {
        return mappingId;
    }

    public MappableTypeDMO getMappableType() {
        return mappableType;
    }

    public String getParentClassOutput() {
        return parentClassOutput;
    }

    public String getParentMethodOutput() {
        return parentMethodOutput;
    }

    public String getParentMethodDescriptor() {
        return parentMethodDescriptor;
    }

    public void setParentMethodDescriptor(final String parentMethodDescriptor) {
        this.parentMethodDescriptor = parentMethodDescriptor;
    }

    public UUID getGameVersionId() {
        return gameVersionId;
    }

    public String getGameVersionName() {
        return gameVersionName;
    }

    public UUID getReleaseId()
    {
        return releaseId;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setDescriptor(final String descriptor) {
        this.descriptor = descriptor;
    }

    public String getOriginalDescriptor() {
        return originalDescriptor;
    }

    public String getDocumentation()
    {
        return documentation;
    }

    public int getIndex()
    {
        return index;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString()
    {
        return "MappingCacheEntry{" +
                 "input='" + input + '\'' +
                 ", output='" + output + '\'' +
                 ", mappableId=" + mappableId +
                 ", versionedMappableId=" + versionedMappableId +
                 ", mappingId=" + mappingId +
                 ", mappableType=" + mappableType +
                 ", parentClassOutput='" + parentClassOutput + '\'' +
                 ", parentMethodOutput='" + parentMethodOutput + '\'' +
                 ", parentMethodDescriptor='" + parentMethodDescriptor + '\'' +
                 ", gameVersionId=" + gameVersionId +
                 ", gameVersionName='" + gameVersionName + '\'' +
                 ", releaseId=" + releaseId +
                 ", type='" + type + '\'' +
                 ", descriptor='" + descriptor + '\'' +
                 ", originalDescriptor='" + originalDescriptor + '\'' +
                 ", documentation='" + documentation + '\'' +
                 ", index=" + index +
                 ", isStatic=" + isStatic +
                 '}';
    }
}
