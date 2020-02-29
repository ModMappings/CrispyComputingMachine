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

    public static final String DELETE_WORKING_DIR_STEP = "downloadWorkingDirStep";
    public static final String DOWNLOAD_MANIFEST_VERSION_STEP_NAME = "downloadManifestVersion";
    public static final String IMPORT_MAPPINGS = "importMappings";

    public static final String WORKING_DIR = "./working";
    public static final String MANIFEST_WORKING_FILE = "version_manifest.json";

    public static final int IMPORT_MAPPINGS_CHUNK_SIZE = 10;

    public static final String OFFICIAL_MAPPING_NAME = "Official";
    public static final String OFFICIAL_MAPPING_STATE_IN = "Obfuscated";
    public static final String OFFICIAL_MAPPING_STATE_OUT = "Official";

    static {
        try {
            MANIFEST_DOWNLOAD_URL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to initialize constants.", e);
        }
    }

}
