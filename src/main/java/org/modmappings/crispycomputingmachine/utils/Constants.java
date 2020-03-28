package org.modmappings.crispycomputingmachine.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public final class Constants {

    private Constants() {
        throw new IllegalStateException("Tried to initialize: Constants but this is a Utility class.");
    }

    public static final UUID SYSTEM_ID = new UUID(0L, 0L);

    public static final URL MANIFEST_DOWNLOAD_URL;
    public static final URL INTERMEDIARY_MAVEN_METADATA_URL;
    public static final URL MCP_CONFIG_MAVEN_METADATA_URL;

    public static final String DOWNLOAD_MANIFEST_VERSION_STEP_NAME = "downloadManifestVersion";
    public static final String DOWNLOAD_INTERMEDIARY_MAVEN_METADATA_STEP_NAME = "downloadIntermediaryMavenMetadata";
    public static final String DOWNLOAD_MCPCONFIG_MAVEN_METADATA_STEP_NAME = "downloadMCPConfigMavenMetadata";
    public static final String IMPORT_MINECRAFT_VANILLA_MAPPINGS = "importMinecraftVanillaMappings";
    public static final String IMPORT_INTERMEDIARY_MAPPINGS = "importIntermediaryMappings";
    public static final String IMPORT_MCPCONFIG_MAPPINGS = "importMCPConfigMappings";

    public static final String MANIFEST_WORKING_FILE = "version_manifest.json";
    public static final String INTERMEDIARY_MAVEN_METADATA_FILE = "intermediary-maven-metadata.xml";
    public static final String MCPCONFIG_MAVEN_METADATA_FILE = "mcpconfig-maven-metadata.xml";

    public static final String OFFICIAL_MAPPING_NAME = "Official";
    public static final String OFFICIAL_MAPPING_STATE_IN = "Obfuscated";
    public static final String OFFICIAL_MAPPING_STATE_OUT = "Official";

    public static final String INTERMEDIARY_MAPPING_NAME = "Intermediary";
    public static final String INTERMEDIARY_MAPPING_STATE_IN = "Obfuscated";
    public static final String INTERMEDIARY_MAPPING_STATE_OUT = "Intermediary";

    public static final String MCP_CONFIG_MAPPING_NAME = "MCP-Config";
    public static final String MCP_CONFIG_MAPPING_STATE_IN = "Obfuscated";
    public static final String MCP_CONFIG_MAPPING_STATE_OUT = "SRG";

    public static final String EXTERNAL_MAPPING_NAME = "External";
    public static final String EXTERNAL_MAPPING_STATE_IN = "External";
    public static final String EXTERNAL_MAPPING_STATE_OUT = "Library";

    public static final String INTERMEDIARY_MAPPING_REPO = "https://maven.fabricmc.net/net/fabricmc/intermediary/";

    public static final String MCP_CONFIG_MAPPING_REPO = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/";

    static {
        try {
            MANIFEST_DOWNLOAD_URL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            INTERMEDIARY_MAVEN_METADATA_URL = new URL(INTERMEDIARY_MAPPING_REPO + "maven-metadata.xml");
            MCP_CONFIG_MAVEN_METADATA_URL = new URL(MCP_CONFIG_MAPPING_REPO + "maven-metadata.xml");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to initialize constants.", e);
        }
    }

}
