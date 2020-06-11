package org.modmappings.crispycomputingmachine.model.mappingtoy;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.MinecraftVersion;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.srgutils.SRGUtilsWrappedMappingFile;

import java.util.Date;
import java.util.Map;

public class MappingToyData {

    private final Map<String, MappingToyJarMetaData.ClassInfo> mappingToyData;
    private final VersionsItem version;
    private final SRGUtilsWrappedMappingFile mergedMappingData;

    public MappingToyData(final Map<String, MappingToyJarMetaData.ClassInfo> mappingToyData, final VersionsItem version, final IMappingFile mergedMappingData) {
        this.mappingToyData = mappingToyData;
        this.version = version;
        this.mergedMappingData = new SRGUtilsWrappedMappingFile(mergedMappingData);
    }

    public Map<String, MappingToyJarMetaData.ClassInfo> getMappingToyData() {
        return mappingToyData;
    }

    public VersionsItem getVersion() {
        return version;
    }

    public SRGUtilsWrappedMappingFile getMergedMappingData() {
        return mergedMappingData;
    }
}
