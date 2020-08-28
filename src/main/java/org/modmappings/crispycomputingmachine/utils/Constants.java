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
    public static final URL MCP_SNAPSHOT_MAVEN_METADATA_URL;
    public static final URL MCP_STABLE_MAVEN_METADATA_URL;
    public static final URL YARN_MAVEN_METADATA_URL;

    public static final String DOWNLOAD_MANIFEST_VERSION_STEP_NAME = "downloadManifestVersion";
    public static final String DOWNLOAD_INTERMEDIARY_MAVEN_METADATA_STEP_NAME = "downloadIntermediaryMavenMetadata";
    public static final String DOWNLOAD_YARN_MAVEN_METADATA_STEP_NAME = "downloadYarnMavenMetadata";
    public static final String DOWNLOAD_MCPCONFIG_MAVEN_METADATA_STEP_NAME = "downloadMCPConfigMavenMetadata";
    public static final String DOWNLOAD_MCPSNAPSHOT_MAVEN_METADATA_STEP_NAME = "downloadMCPSnapshotMavenMetadata";
    public static final String DOWNLOAD_MCPSTABLE_MAVEN_METADATA_STEP_NAME = "downloadMCPStableMavenMetadata";
    public static final String IMPORT_MINECRAFT_VANILLA_MAPPINGS = "importMinecraftVanillaMappings";
    public static final String IMPORT_INTERMEDIARY_MAPPINGS = "importIntermediaryMappings";
    public static final String IMPORT_YARN_MAPPINGS = "importIntermediaryMappings";
    public static final String IMPORT_MCPCONFIG_MAPPINGS = "importMCPConfigMappings";

    public static final String MANIFEST_WORKING_FILE = "version_manifest.json";
    public static final String INTERMEDIARY_MAVEN_METADATA_FILE = "intermediary-maven-metadata.xml";
    public static final String YARN_MAVEN_METADATA_FILE = "yarn-maven-metadata.xml";
    public static final String MCP_CONFIG_MAVEN_METADATA_FILE = "mcpconfig-maven-metadata.xml";
    public static final String MCP_SNAPSHOT_MAVEN_METADATA_FILE = "mcpsnapshot-maven-metadata.xml";
    public static final String MCP_STABLE_MAVEN_METADATA_FILE = "mcpstable-maven-metadata.xml";

    public static final String OFFICIAL_MAPPING_NAME = "Official";
    public static final String OFFICIAL_MAPPING_STATE_IN = "Obfuscated";
    public static final String OFFICIAL_MAPPING_STATE_OUT = "Official";

    public static final String INTERMEDIARY_MAPPING_NAME = "Intermediary";
    public static final String INTERMEDIARY_MAPPING_STATE_IN = "Obfuscated";
    public static final String INTERMEDIARY_MAPPING_STATE_OUT = "Intermediary";

    public static final String MCP_CONFIG_MAPPING_NAME = "MCP-Config";
    public static final String MCP_CONFIG_MAPPING_STATE_IN = "Obfuscated";
    public static final String MCP_CONFIG_MAPPING_STATE_OUT = "SRG";

    public static final String YARN_MAPPING_NAME = "Yarn";
    public static final String YARN_MAPPING_STATE_IN = "Intermediary";
    public static final String YARN_MAPPING_STATE_OUT = "Yarn";

    public static final String EXTERNAL_MAPPING_NAME = "External";
    public static final String EXTERNAL_MAPPING_STATE_IN = "External";
    public static final String EXTERNAL_MAPPING_STATE_OUT = "Library";

    public static final String MCP_MAPPING_NAME = "MCP";
    public static final String MCP_MAPPING_STATE_IN = "SRG";
    public static final String MCP_MAPPING_STATE_OUT = "MCP";
    
    public static final String INTERMEDIARY_MAPPING_REPO = "https://maven.fabricmc.net/net/fabricmc/intermediary/";
    public static final String INTERMEDIARY_JAR = "intermediary.jar";
    public static final String INTERMEDIARY_WORKING_DIR = "intermediary";

    public static final String MCP_CONFIG_MAPPING_REPO = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/";
    public static final String MCP_CONFIG_ZIP = "mcp-config.zip";
    public static final String MCP_CONFIG_WORKING_DIR = "mcp_config";

    public static final String YARN_MAPPING_REPO = "https://maven.fabricmc.net/net/fabricmc/yarn/";
    public static final String YARN_JAR = "yarn.jar";
    public static final String YARN_WORKING_DIR = "yarn";

    public static final String MCP_STABLE_MAPPING_REPO = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_stable/";
    public static final String MCP_STABLE_JAR = "mcp_stable.jar";
    public static final String MCP_STABLE_WORKING_DIR = "mcp_stable";
    public static final String MCP_STABLE_ARTIFACT = "mcp_stable";

    public static final String MCP_SNAPSHOT_MAPPING_REPO = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_snapshot/";
    public static final String MCP_SNAPSHOT_JAR = "mcp_snapshot.jar";
    public static final String MCP_SNAPSHOT_WORKING_DIR = "mcp_snapshot";
    public static final String MCP_SNAPSHOT_ARTIFACT = "mcp_snapshot";

    static {
        try {
            MANIFEST_DOWNLOAD_URL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            INTERMEDIARY_MAVEN_METADATA_URL = new URL(INTERMEDIARY_MAPPING_REPO + "maven-metadata.xml");
            MCP_CONFIG_MAVEN_METADATA_URL = new URL(MCP_CONFIG_MAPPING_REPO + "maven-metadata.xml");
            MCP_SNAPSHOT_MAVEN_METADATA_URL = new URL(MCP_SNAPSHOT_MAPPING_REPO + "maven-metadata.xml");
            MCP_STABLE_MAVEN_METADATA_URL = new URL(MCP_STABLE_MAPPING_REPO + "maven-metadata.xml");
            YARN_MAVEN_METADATA_URL = new URL(YARN_MAPPING_REPO + "maven-metadata.xml");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to initialize constants.", e);
        }
    }

}
