package org.modmappings.crispycomputingmachine.processors;

import com.google.gson.reflect.TypeToken;
import net.minecraftforge.lex.mappingtoy.MappingToy;
import net.minecraftforge.lex.mappingtoy.Utils;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.MinecraftVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyData;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyJarMetaData;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;

public class MappingToyInformationExtractor implements ItemProcessor<MinecraftVersion, MappingToyData>, InitializingBean {

    private static final Logger LOGGER = LogManager.getLogger(MappingToyInformationExtractor.class);

    private Resource workingDirectoryResource;

    @Override
    public MappingToyData process(final MinecraftVersion item) throws Exception {
        LOGGER.info("Processing Version with MappingToy: " + item.toString());
        final File workingDir = new File(workingDirectoryResource.getFile().getAbsolutePath());
        Assert.state(workingDir.exists(), "Working directory does not exist: " + workingDir.getAbsolutePath());
        Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());

        final File mapDataDirectory = new File(workingDir, "map_data");
        final File minecraftDirectory = new File(workingDir, "minecraft");

        if (!mapDataDirectory.exists()) {
            Assert.state(mapDataDirectory.mkdirs(), "Failed to create target directory for mapping data: " + item.toString());
        }

        if (!minecraftDirectory.exists()) {
            Assert.state(minecraftDirectory.mkdirs(), "Failed to created target directory for minecraft: " + item.toString());
        }

        final LinkedList<String> mappingToyArgs = new LinkedList<>();
        mappingToyArgs.add("--libs");
        mappingToyArgs.add("--output");
        mappingToyArgs.add(mapDataDirectory.getAbsolutePath());
        mappingToyArgs.add("--mc");
        mappingToyArgs.add(minecraftDirectory.getAbsolutePath());
        mappingToyArgs.add("--version");
        mappingToyArgs.add(item.toString());
        mappingToyArgs.add("--force");

        LOGGER.info("Executing MappingToy for: " + item.toString());
        //Invoke mapping toy.
        MappingToy.main(mappingToyArgs.toArray(new String[mappingToyArgs.size()]));

        final File versionedMapDataDirectory = new File(mapDataDirectory, item.toString());

        final File metadataFile = new File(versionedMapDataDirectory, "joined_a_meta.json");
        final File mappingDataFile = new File(versionedMapDataDirectory, "joined_o_to_n.tsrg");

        if (!metadataFile.exists())
        {
            //Seems like something went wrong.
            LOGGER.warn("Failed to execute mapping toy. Metadata is missing for: " + item.toString());
            return null;
        }

        if (!mappingDataFile.exists())
        {
            //Again this file is needed.
            LOGGER.warn("Failed to execute mapping toy. Mapping data is missing for: " + item.toString());
            return null;
        }

        try (InputStream in = Files.newInputStream(metadataFile.toPath())) {
            LOGGER.warn("Mapping Toy completed for: " + item.toString());
            Map<String, MappingToyJarMetaData.ClassInfo> resultData = Utils.GSON.fromJson(new InputStreamReader(in), new TypeToken<TreeMap<String, MappingToyJarMetaData.ClassInfo>>(){}.getType());
            return new MappingToyData(resultData, item, IMappingFile.load(mappingDataFile));
        }
    }

    public void setWorkingDirectoryResource(final Resource workingDirectoryResource) {
        this.workingDirectoryResource = workingDirectoryResource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectoryResource, "Working directory is not set!");
    }
}
