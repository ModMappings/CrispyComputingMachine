package org.modmappings.crispycomputingmachine.cache;

import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.UUID;

public class MappingCacheEntry {

    private String input;
    private String output;
    private UUID mappableId;
    private UUID versionedMappableId;
    private MappableTypeDMO mappableType;
    private String parentClassOutput;
    private String parentMethodOutput;
    private UUID gameVersionId;
    private String gameVersionName;
    private String type;
    private String descriptor;
    private boolean isStatic;

    public MappingCacheEntry(final String input, final String output, final UUID mappableId, final UUID versionedMappableId, final MappableTypeDMO mappableType, final String parentClassOutput, final String parentMethodOutput, final UUID gameVersionId, final String gameVersionName, final String type, final String descriptor, final boolean isStatic) {
        this.input = input;
        this.output = output;
        this.mappableId = mappableId;
        this.versionedMappableId = versionedMappableId;
        this.mappableType = mappableType;
        this.parentClassOutput = parentClassOutput;
        this.parentMethodOutput = parentMethodOutput;
        this.gameVersionId = gameVersionId;
        this.gameVersionName = gameVersionName;
        this.type = type;
        this.descriptor = descriptor;
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

    public MappableTypeDMO getMappableType() {
        return mappableType;
    }

    public String getParentClassOutput() {
        return parentClassOutput;
    }

    public String getParentMethodOutput() {
        return parentMethodOutput;
    }

    public UUID getGameVersionId() {
        return gameVersionId;
    }

    public String getGameVersionName() {
        return gameVersionName;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
