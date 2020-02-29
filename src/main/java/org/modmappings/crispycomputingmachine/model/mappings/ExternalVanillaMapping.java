package org.modmappings.crispycomputingmachine.model.mappings;

import java.util.Date;

public class ExternalVanillaMapping {

    private String input;
    private String output;
    private ExternalMappableType mappableTypeDMO;

    private String gameVersion;
    private Date gameVersionReleaseDate;

    private String parentClassMapping;
    private String parentMethodMapping;

    private ExternalVisibility visibility;
    private boolean isStatic;
    private String type;
    private String descriptor;

    public ExternalVanillaMapping(final String input, final String output, final ExternalMappableType mappableTypeDMO, final String gameVersion, final Date gameVersionReleaseDate, final String parentClassMapping, final String parentMethodMapping, final ExternalVisibility visibility, final boolean isStatic, final String type, final String descriptor) {
        this.input = input;
        this.output = output;
        this.mappableTypeDMO = mappableTypeDMO;
        this.gameVersion = gameVersion;
        this.gameVersionReleaseDate = gameVersionReleaseDate;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.type = type;
        this.descriptor = descriptor;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public ExternalMappableType getMappableType() {
        return mappableTypeDMO;
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
}
