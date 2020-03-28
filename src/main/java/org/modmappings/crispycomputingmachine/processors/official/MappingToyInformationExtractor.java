package org.modmappings.crispycomputingmachine.processors.official;

import com.google.gson.reflect.TypeToken;
import net.minecraftforge.lex.mappingtoy.MappingToy;
import net.minecraftforge.lex.mappingtoy.Utils;
import net.minecraftforge.srgutils.IMappingFile;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyData;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyJarMetaData;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;

@Component
public class MappingToyInformationExtractor implements ItemProcessor<VersionsItem, MappingToyData>, InitializingBean {

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public MappingToyData process(final VersionsItem item) throws Exception {
        final File workingDir = new File(workingDirectory.getFile().getAbsolutePath());
        Assert.state(workingDir.exists(), "The working directory does not exist: " + workingDir.getAbsolutePath());
        Assert.state(workingDir.isDirectory(), "The working directory is not a directory: " + workingDir.getAbsolutePath());

        final File mapDataDirectory = new File(workingDir, "map_data");
        final File minecraftDirectory = new File(workingDir, "minecraft");

        if (!mapDataDirectory.exists()) {
            Assert.state(mapDataDirectory.mkdirs(), "Failed to create target directory for mapping data: " + item.getId());
        }

        if (!minecraftDirectory.exists()) {
            Assert.state(minecraftDirectory.mkdirs(), "Failed to created target directory for minecraft: " + item.getId());
        }

        final LinkedList<String> mappingToyArgs = new LinkedList<>();
        mappingToyArgs.add("--libs");
        mappingToyArgs.add("--output");
        mappingToyArgs.add(mapDataDirectory.getAbsolutePath());
        mappingToyArgs.add("--mc");
        mappingToyArgs.add(minecraftDirectory.getAbsolutePath());
        mappingToyArgs.add("--version");
        mappingToyArgs.add(item.getId());
        mappingToyArgs.add("--force");

        //Invoke mapping toy.
        MappingToy.main(mappingToyArgs.toArray(new String[0]));

        final File versionedMapDataDirectory = new File(mapDataDirectory, item.getId());

        final File metadataFile = new File(versionedMapDataDirectory, "joined_a_meta.json");
        final File clientMappingFile = new File(versionedMapDataDirectory, "client.txt");

        if (!metadataFile.exists())
        {
            //Seems like something went wrong.
            return null;
        }

        if (!clientMappingFile.exists())
        {
            //Again this file is needed.
            return null;
        }

        try (InputStream in = Files.newInputStream(metadataFile.toPath())) {
            Map<String, MappingToyJarMetaData.ClassInfo> resultData = Utils.GSON.fromJson(new InputStreamReader(in), new TypeToken<TreeMap<String, MappingToyJarMetaData.ClassInfo>>(){}.getType());
            return new MappingToyData(resultData, item, IMappingFile.load(clientMappingFile).reverse());
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(workingDirectory, "Working directory is not set!");
    }
}
