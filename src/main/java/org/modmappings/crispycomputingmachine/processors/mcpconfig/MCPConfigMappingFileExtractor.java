package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import net.minecraftforge.srgutils.IMappingFile;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.srgutils.SRGUtilsWrappedMappingFile;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MCPConfigMappingFileExtractor implements ItemProcessor<String, List<ExternalMapping>> {

    @Value("${importer.directories.working:file:working}")
    Resource workingDirectory;

    @Override
    public List<ExternalMapping> process(final String item) throws Exception {
        final File workingDirectoryFile = workingDirectory.getFile();
        workingDirectoryFile.mkdirs();
        final File versionWorkingDirectory = new File(workingDirectoryFile, item);
        versionWorkingDirectory.mkdirs();
        final File unzippingMappingJarTarget = new File(versionWorkingDirectory, "mcp-config");
        final File mappingsDirectory = new File(unzippingMappingJarTarget,"config");
        final File joinedMappingFile = new File(mappingsDirectory, "joined.tsrg");

        final SRGUtilsWrappedMappingFile mappingFile = new SRGUtilsWrappedMappingFile(IMappingFile.load(joinedMappingFile));

        final String gameVersion = item.split("-")[0];

        final Map<String, ExternalMapping> classes = mappingFile.getMappingFile().getClasses().stream().map(cls -> {
            String parentClassOut = null;
            if (cls.getMapped().contains("$"))
                parentClassOut = cls.getMapped().substring(0, cls.getMapped().indexOf("$"));

            return new ExternalMapping(
                    cls.getOriginal(),
                    cls.getMapped(),
                    ExternalMappableType.CLASS,
                    gameVersion,
                    item,
                    parentClassOut,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        })
        .collect(Collectors.toMap(ExternalMapping::getInput, Function.identity()));

        final List<ExternalMapping> methods = mappingFile.getMappingFile().getClasses().parallelStream().flatMap(cls -> cls.getMethods().parallelStream())
                .map(method -> {
                    final String parentClassOut = method.getParent().getMapped();

                    return new ExternalMapping(
                            method.getOriginal(),
                            method.getMapped(),
                            ExternalMappableType.METHOD,
                            gameVersion,
                            item,
                            parentClassOut,
                            null,
                            null,
                            null,
                            method.getDescriptor(),
                            null
                    );
                }).collect(Collectors.toList());

        final List<ExternalMapping> fields = mappingFile.getMappingFile().getClasses().parallelStream().flatMap(cls -> cls.getFields().parallelStream())
                .map(field -> {
                    final String parentClassOut = field.getParent().getMapped();

                    return new ExternalMapping(
                            field.getOriginal(),
                            field.getMapped(),
                            ExternalMappableType.FIELD,
                            gameVersion,
                            item,
                            parentClassOut,
                            null,
                            null,
                            "*", //I do not have any information on the type of the field in TSRG mapping files. Additionally the type is irrelevant here and only used for cache lookups. The mapping key which is used as cache key, knows that * means do not care, just use names to match.
                            null,
                            null
                    );
                }).collect(Collectors.toList());

        final List<ExternalMapping> results = new ArrayList<>(classes.values());
        results.addAll(methods);
        results.addAll(fields);

        return results;
    }
}
