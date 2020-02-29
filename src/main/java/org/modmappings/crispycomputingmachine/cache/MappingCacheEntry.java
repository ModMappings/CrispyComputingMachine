package org.modmappings.crispycomputingmachine.cache;

import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.UUID;

public class MappingCacheEntry {

    private String output;
    private UUID mappingId;
    private UUID versionedMappableId;
    private MappableTypeDMO mappableTypeDMO;
    private String parentClassOutput;
    private String parentMethodOutput;
    private UUID gameVersionedId;
    private String gameVersionName;

    public MappingCacheEntry(final String output, final UUID mappingId, final UUID versionedMappableId, final MappableTypeDMO mappableTypeDMO, final String parentClassOutput, final String parentMethodOutput, final UUID gameVersionedId, final String gameVersionName) {
        this.output = output;
        this.mappingId = mappingId;
        this.versionedMappableId = versionedMappableId;
        this.mappableTypeDMO = mappableTypeDMO;
        this.parentClassOutput = parentClassOutput;
        this.parentMethodOutput = parentMethodOutput;
        this.gameVersionedId = gameVersionedId;
        this.gameVersionName = gameVersionName;
    }

    public String getOutput() {
        return output;
    }

    public UUID getMappingId() {
        return mappingId;
    }

    public UUID getVersionedMappableId() {
        return versionedMappableId;
    }

    public MappableTypeDMO getMappableTypeDMO() {
        return mappableTypeDMO;
    }

    public String getParentClassOutput() {
        return parentClassOutput;
    }

    public String getParentMethodOutput() {
        return parentMethodOutput;
    }

    public UUID getGameVersionedId() {
        return gameVersionedId;
    }

    public String getGameVersionName() {
        return gameVersionName;
    }
}
