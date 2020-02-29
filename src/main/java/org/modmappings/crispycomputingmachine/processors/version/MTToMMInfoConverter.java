package org.modmappings.crispycomputingmachine.processors.version;

import net.minecraftforge.srgutils.MinecraftVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappings.*;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyData;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyJarMetaData;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MTToMMInfoConverter implements ItemProcessor<MappingToyData, ExternalRelease> {

    private static final Logger LOGGER = LogManager.getLogger(MTToMMInfoConverter.class);

    @Override
    public ExternalRelease process(final MappingToyData item) {
        LOGGER.info("Converting MappingToy data to ModMappings data for version: " + item.getVersion().toString());
        final Map<String, ExternalClass> inputToClassMappingData = new ConcurrentHashMap<>();

        //Okey this processes all class to get the correct references later.
        item.getMappingToyData().entrySet().parallelStream()
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(
                (e) -> {
                    final String obfName = e.getKey();
                    final MappingToyJarMetaData.ClassInfo classData = e.getValue();
                    LOGGER.info("[" + item.getVersion().toString() + "] Creating Obf Class: " + obfName);
                    inputToClassMappingData.put(
                            obfName,
                            new ExternalClass(
                                    obfName,
                                    item.getMergedMappingData().remapClass(obfName),
                                    new HashSet<>(),
                                    new HashSet<>(),
                                    new HashSet<>(),
                                    toVisibility(classData),
                                    classData.isStatic(),
                                    classData.isAbstract(),
                                    classData.isInterface(),
                                    classData.isEnum()
                            )
                    );
                }
        );

        item.getMappingToyData().entrySet().stream()
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(e -> item.getMergedMappingData().findClassFromName(e.getKey()));

        //Now loop over all classes again to populate the correct super classes, interfaces, methods etc
        item.getMappingToyData().entrySet().parallelStream()
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(
                (e) -> {
                    final String obfClassName = e.getKey();
                    final MappingToyJarMetaData.ClassInfo classData = e.getValue();
                    final ExternalClass target = inputToClassMappingData.get(obfClassName);
                    final String superName = classData.getSuper();
                    if (!superName.equals("java/lang/Object") && inputToClassMappingData.containsKey(superName))
                    {
                        final ExternalClass superClass = inputToClassMappingData.get(superName);
                        target.getSuperClasses().add(superClass);
                    }

                    target.getSuperClasses().addAll(classData.getInterfaces().stream().filter(inputToClassMappingData::containsKey).map(inputToClassMappingData::get).collect(Collectors.toList()));

                    target.getMethods().addAll(
                            classData.getMethods().entrySet().stream().map(
                                    (entry) -> new ExternalMethod(
                                            entry.getKey(),
                                            item.getMergedMappingData().findClassFromName(obfClassName).remapMethod(entry.getKey(), entry.getValue().getSignature()),
                                            toVisibility(entry.getValue()),
                                            entry.getValue().isStatic(),
                                            entry.getValue().getSignature()
                                    )
                            ).collect(Collectors.toList())
                    );

                    target.getFields().addAll(
                            classData.getFields().entrySet().stream().map(
                                    (entry) -> new ExternalField(
                                            entry.getKey(),
                                            item.getMergedMappingData().findClassFromName(obfClassName).remapField(entry.getKey()),
                                            entry.getValue().getSignature(),
                                            toVisibility(entry.getValue()),
                                            entry.getValue().isStatic())
                            ).collect(Collectors.toList())
                    );
                }
        );

        return new ExternalRelease(
                item.getVersion().toString(),
                Date.from(Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(item.getVersion().getReleaseTime()))), new LinkedList<>(inputToClassMappingData.values()),
                isPreRelease(item.getVersion()), isSnapshot(item.getVersion()));
    }

    private static ExternalVisibility toVisibility(MappingToyJarMetaData.IAccessible access)
    {
        if (access.isPublic())
            return ExternalVisibility.PUBLIC;
        if (access.isPackagePrivate())
            return ExternalVisibility.PACKAGE;
        if (access.isProtected())
            return ExternalVisibility.PROTECTED;
        if (access.isPrivate())
            return ExternalVisibility.PRIVATE;

        return ExternalVisibility.UNKNOWN;
    }
    
    private static boolean isPreRelease(VersionsItem version)
    {
        String lower = version.getId().toLowerCase(Locale.ENGLISH);

        if ("15w14a".equals(lower)) { //2015 April Fools
            return false;
        } else if ("1.rv-pre1".equals(lower)) { //2016 April Fools
            return false;
        } else if ("3d shareware v1.34".equals(lower)) { //2019 April Fools
            return false;
        } else if (lower.charAt(0) == 'b' || lower.charAt(0) == 'a') {
            return false;
        } else if (lower.length() == 6 && lower.charAt(2) == 'w') {
            return false;
        } else {
            if (lower.contains("-pre")) {
                return true;
            } else if (lower.contains("_Pre-Release_".toLowerCase())) {
                return true;
            } else return lower.contains(" Pre-Release ".toLowerCase());
        }
    }

    private static boolean isSnapshot(VersionsItem version)
    {
        String lower = version.getId().toLowerCase(Locale.ENGLISH);
        switch (lower) {
            case "15w14a":  //2015 April Fools
                return true;
            case "1.rv-pre1":  //2016 April Fools
                return true;
            case "3d shareware v1.34":  //2019 April Fools
                return true;
            default:
                return lower.length() == 6 && lower.charAt(2) == 'w';
        }
    }
}
