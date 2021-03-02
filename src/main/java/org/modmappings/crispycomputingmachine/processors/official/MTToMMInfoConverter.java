package org.modmappings.crispycomputingmachine.processors.official;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.srgutils.IMappingFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.launcher.VersionsItem;
import org.modmappings.crispycomputingmachine.model.mappings.*;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyData;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyJarMetaData;
import org.modmappings.crispycomputingmachine.utils.MethodRef;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MTToMMInfoConverter implements ItemProcessor<MappingToyData, ExternalRelease> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public ExternalRelease process(final MappingToyData item) {
        final Map<String, ExternalClass> inputToClassMappingData = new ConcurrentHashMap<>();
        final Map<String, ExternalClass> outputToClassMappingData = new ConcurrentHashMap<>();

        //Okey this processes all class to get the correct references later.
        item.getMappingToyData().entrySet().parallelStream()
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(
                (e) -> {
                    final String obfName = e.getKey();
                    final MappingToyJarMetaData.ClassInfo classData = e.getValue();
                    final ExternalClass clz = new ExternalClass(
                            obfName,
                            item.getMergedMappingData().remapClass(obfName),
                            new HashSet<>(),
                            new HashSet<>(),
                            new HashSet<>(),
                            toVisibility(classData),
                            classData.isStatic(),
                            classData.isAbstract(),
                            classData.isInterface(),
                            classData.isEnum(),
                            false);

                    inputToClassMappingData.put(
                            clz.getInput(),
                            clz
                    );
                    outputToClassMappingData.put(
                            clz.getOutput(),
                            clz
                    );
                }
        );

        //This deals with marker interfaces.
        item.getMergedMappingData().getMappingFile().getClasses().parallelStream()
                .filter(cls -> cls.getFields().isEmpty() && cls.getMethods().isEmpty())
                .forEach(cls -> {
                    final ExternalClass clz = new ExternalClass(
                            cls.getOriginal(),
                            cls.getMapped(),
                            new HashSet<>(),
                            new HashSet<>(),
                            new HashSet<>(),
                            ExternalVisibility.PUBLIC,
                            false,
                            false,
                            true,
                            false,
                            false);

                    inputToClassMappingData.put(
                            clz.getInput(),
                            clz
                    );
                    outputToClassMappingData.put(
                            clz.getOutput(),
                            clz
                    );
                });

        item.getMappingToyData().entrySet().stream()
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(e -> item.getMergedMappingData().findClassFromName(e.getKey()));

        //Loop over all class again to collect override information on methods so that override logic
        //Can properly find the correct overrides. This is needed because the obfuscator can
        //reuse method names and descriptors in super classes which might add them to the inheritance tree
        //of a particular method, which is not actually part of the same inheritance logic.
        //
        //Mapping toy, provides the root information, but we are interested in the entire tree,
        //So we can collect all of them, and when we walk the tree we know which ones are considered
        //to be part of that methods particular tree:
        final Multimap<MethodRef, MethodRef> methodInheritanceData = HashMultimap.create();
        item.getMappingToyData().entrySet().stream().sorted(Map.Entry.comparingByKey())
          .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
          .forEach(e -> {
              final String obfClassName = e.getKey();

              e.getValue().getMethods().forEach((key, value) -> {
                  final String obfMethodName = key.substring(0, key.indexOf("("));
                  final String obfSignature = key.substring(obfMethodName.length());

                  final MethodRef targetRef = new MethodRef(obfClassName, obfMethodName, obfSignature);
                  value.getOverrides().forEach(override -> methodInheritanceData.put(override, targetRef));
              });
          });

        //Now loop over all classes again to populate the correct super classes, interfaces, methods etc
        item.getMappingToyData().entrySet().stream().sorted(
                Map.Entry.comparingByKey()
                )
                .filter(e -> !e.getKey().startsWith("net/minecraftforge"))
                .forEach(
                (e) -> {
                    final String obfClassName = e.getKey();
                    final MappingToyJarMetaData.ClassInfo classData = e.getValue();
                    final ExternalClass target = inputToClassMappingData.get(obfClassName);

                    if(target.getOutput().contains("$")) {
                        final String parentClassOutputMapping = target.getOutput().substring(0, target.getOutput().lastIndexOf("$"));
                        handlePotentiallyExternalClass(parentClassOutputMapping, true, false, item, inputToClassMappingData, outputToClassMappingData);
                    }

                    final String superName = classData.getSuper();
                    if (!superName.equals("java/lang/Object"))
                    {
                        handlePotentiallyExternalClass(superName, false, false, item, inputToClassMappingData, outputToClassMappingData);
                        final ExternalClass superClass = inputToClassMappingData.get(superName);
                        target.getSuperClasses().add(superClass);
                    }

                    classData.getInterfaces().forEach(ifName -> {
                        handlePotentiallyExternalClass(ifName, false, true, item, inputToClassMappingData, outputToClassMappingData);
                        final ExternalClass superInterface = inputToClassMappingData.get(ifName);
                        target.getSuperClasses().add(superInterface);
                    });

                    target.getMethods().addAll(
                            classData.getMethods().entrySet().stream().map(
                                    (entry) -> {
                                        final String obfMethodName = entry.getKey().substring(0, entry.getKey().indexOf("("));
                                        final String obfSignature = entry.getKey().substring(obfMethodName.length());

                                        final Collection<MethodRef> validCandidates = entry.getValue().getOverrides().stream().flatMap(ref -> {
                                            return methodInheritanceData.get(ref).stream();
                                        }).collect(Collectors.toSet());
                                        final Set<MethodOverrideRef> overrides = findAllOverrideReferenceFrom(entry.getKey(), obfClassName, entry.getValue(), classData, item, validCandidates);
                                        overrides.forEach(methodRef -> {
                                            handlePotentiallyExternalClass(methodRef.getOwner(), false, methodRef.isFromInterface(), item, inputToClassMappingData, outputToClassMappingData);
                                        });

                                        return new ExternalMethod(
                                                obfMethodName,
                                                item.getMergedMappingData().findClassFromName(obfClassName).remapMethod(obfMethodName, obfSignature),
                                                toVisibility(entry.getValue()),
                                                entry.getValue().isStatic(),
                                                obfSignature,
                                                entry.getValue().getSignature(),
                                                false,
                                                //entry.getValue().getOverrides()
                                                overrides.stream().map(mr -> mr.remap(item.getMergedMappingData())).collect(Collectors.toSet())
                                        );
                                    }
                            ).collect(Collectors.toList())
                    );

                    final IMappingFile.IClass mappingClass = item.getMergedMappingData().findClassFromName(obfClassName);
                    final Map<String, IMappingFile.IField> fields = mappingClass.getFields().stream()
                            .collect(Collectors.toMap(IMappingFile.INode::getOriginal, Function.identity()));

                    target.getFields().addAll(
                            classData.getFields().entrySet().stream().map(
                                    (entry) -> new ExternalField(
                                            entry.getKey(),
                                            item.getMergedMappingData().findClassFromName(obfClassName).remapField(entry.getKey()),
                                            fields.get(entry.getKey()).getDescriptor(),
                                            toVisibility(entry.getValue()),
                                            entry.getValue().isStatic())
                            ).collect(Collectors.toList())
                    );
                }
        );

        addExternalMethodsToExternalClassesFromOverrides(outputToClassMappingData);

        return new ExternalRelease(
                item.getVersion().getId(),
                Date.from(Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(item.getVersion().getReleaseTime()))),
                new LinkedList<>(inputToClassMappingData.values()),
                isPreRelease(item.getVersion()), isSnapshot(item.getVersion()),
                item.getClientData(),
                item.getServerData());
    }

    private static void addExternalMethodsToExternalClassesFromOverrides(final Map<String, ExternalClass> outputMappingAndClassesToCheck)
    {
        outputMappingAndClassesToCheck.values().forEach(externalClass -> {
            externalClass.getMethods().forEach(externalMethod -> {
                externalMethod.getOverrides().forEach(methodOverride -> {
                    final ExternalClass overridenClass = outputMappingAndClassesToCheck.get(methodOverride.getOwner()); //Has to exist since external classes have been added previously.
                    overridenClass.getMethods().stream().filter(methodToCheck -> methodToCheck.getOutput().equals(methodOverride.getName()) && methodToCheck.getDescriptor().equals(methodOverride.getDesc())).findFirst().ifPresentOrElse(
                            (em) -> {
                                //Noop, we do not care if the method already exists.
                            },
                            () -> {
                                //This should only happen on external classes.
                                //Lets log however so we can make sure that this is really the case.
                                LOGGER.debug("Creating external method reference: " + methodOverride);

                                final ExternalMethod newExternalMethod = new ExternalMethod(
                                        methodOverride.getName(),
                                        methodOverride.getName(),
                                        ExternalVisibility.PUBLIC,
                                        false,
                                        methodOverride.getDesc(),
                                        null,
                                        true,
                                        new HashSet<>()
                                );

                                overridenClass.getMethods().add(newExternalMethod);
                            }
                    );
                });
            });
        });
    }

    private static Set<MethodOverrideRef> findAllOverrideReferenceFrom(final String methodId, final String parentClassName, final MappingToyJarMetaData.ClassInfo.MethodInfo methodInfo, final MappingToyJarMetaData.ClassInfo parentClass, final MappingToyData data, final Collection<MethodRef> validCandidates)
    {
        if (methodInfo.getOverrides().isEmpty())
            return Collections.emptySet();

        final Set<MethodOverrideRef> references = new HashSet<>();

        methodInfo.getOverrides().forEach(override -> {
            handleInheritanceOverridingIn(methodId, parentClassName, data, references, override, parentClass.getSuper(), false, validCandidates);
            parentClass.getInterfaces().forEach(interfaceName -> handleInheritanceOverridingIn(methodId, parentClassName, data, references, override, interfaceName, true,
              validCandidates));
        });

        if (references.isEmpty())
        {
            LOGGER.debug("Could not find any overriden methods for: " + methodId + " in superclass or interface of: " + parentClassName + ".");
            LOGGER.debug("However the following where defined:");
            methodInfo.getOverrides().forEach(override ->{
                LOGGER.debug(" - " + override.getName() + override.getDesc() + " in: " + override.getOwner());
            });
            methodInfo.getOverrides().stream().map(mr -> new MethodOverrideRef(mr.getOwner(), mr.getName(), mr.getDesc(), parentClass.isInterface())).forEach(references::add);
            return references;
        }

        return references;
    }

    private static void handleInheritanceOverridingIn(
      final String methodId,
      final String parentClassName,
      final MappingToyData data,
      final Set<MethodOverrideRef> references,
      final MethodRef override,
      String superName,
      boolean isInterface,
      final Collection<MethodRef> validCandidates) {
        while(data.getMappingToyData().containsKey(superName))
        {
            final MappingToyJarMetaData.ClassInfo superClassInfo = data.getMappingToyData().get(superName);

            final String methodName = methodId.substring(0, methodId.indexOf("("));

            final Optional<String> superMethodInfo = superClassInfo.getMethods().keySet()
                                                                     .stream().filter(mi -> {
                                final MappingToyJarMetaData.ClassInfo.MethodInfo superInfo = superClassInfo.getMethods().get(mi);
                                if (!superInfo.getOverrides().contains(override))
                                    return false;

                                final MethodRef candidateRef = new MethodRef(superClassInfo, superInfo);
                                if (!validCandidates.contains(candidateRef))
                                    return false;

                                return methodId.equals(mi);
                            }).findFirst();

            if (superMethodInfo.isPresent())
            {
                LOGGER.debug("Overriding method: "+ methodId + " from: " + parentClassName + " with " + (isInterface ? "interface" : "superclass") +" : " + superName + " and method: " + superMethodInfo.get());

                references.add(
                        new MethodOverrideRef(
                                superName,
                                superMethodInfo.get().substring(0, superMethodInfo.get().indexOf("(")),
                                superMethodInfo.get().substring(superMethodInfo.get().indexOf("(")),
                                isInterface
                        )
                );

                return;
            }
            else
            {
                //If it is empty, we might be at the root and actually do not have a super that has that method defined.
                //Check if we potentially have the method with the correct descriptor and name?
                if (superClassInfo.getMethods().containsKey(methodId) && superName.equals(override.getOwner()))
                {
                    LOGGER.debug("Overriding method: "+ methodId + " from: " + parentClassName + " with " + (isInterface ? "interface" : "superclass") +" : " + superName + " and method: " + methodId);

                    references.add(
                            new MethodOverrideRef(
                                    superName,
                                    methodId.substring(0, methodId.indexOf("(")),
                                    methodId.substring(methodId.indexOf("(")),
                                            isInterface
                            )
                    );

                    return;
                }
                else if (superName.equals(override.getOwner()) && superClassInfo.getMethods().containsKey(override.getName() + override.getDesc()))
                {
                    final String overrideId = override.getName() + override.getDesc();
                    LOGGER.debug("Overriding method: "+ methodId + " from: " + parentClassName + " with " + (isInterface ? "interface" : "superclass") +" : " + superName + " and method: " + overrideId);

                    references.add(
                            new MethodOverrideRef(
                                    superName,
                                    overrideId.substring(0, overrideId.indexOf("(")),
                                    overrideId.substring(overrideId.indexOf("(")),
                                            isInterface
                            )
                    );

                    return;
                }

                superName = superClassInfo.getSuper();
                superClassInfo.getInterfaces().forEach(interfaceName -> handleInheritanceOverridingIn(methodId, parentClassName, data, references, override, interfaceName, true,
                  validCandidates));
            }
        }
    }

    private static void handlePotentiallyExternalClass(
            String mapping,
            boolean isOutput,
            boolean isInterface,
            MappingToyData data,
            Map<String, ExternalClass> inputClasses,
            Map<String, ExternalClass> outputClasses)
    {
        ExternalClass existing = isOutput ? outputClasses.get(mapping) : inputClasses.get(mapping);

        if (existing == null)
        {
            final String input = mapping;
            final String output = isOutput ? mapping : data.getMergedMappingData().remapClass(mapping);

            existing = new ExternalClass(
                    input,
                    output,
                    new HashSet<>(),
                    new HashSet<>(),
                    new HashSet<>(),
                    ExternalVisibility.PUBLIC,
                    false,
                    false,
                    isInterface,
                    false,
                    true
            );

            inputClasses.put(input, existing);
            outputClasses.put(output, existing);
        }

        if (mapping.contains("$"))
        {
            final String parentName = mapping.substring(0, mapping.lastIndexOf("$"));
            handlePotentiallyExternalClass(parentName, isOutput, isInterface, data, inputClasses, outputClasses);
        }
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

    private static class MethodOverrideRef extends MethodRef {

        private final boolean isFromInterface;

        public MethodOverrideRef(final String owner, final String name, final String desc, final boolean isFromInterface) {
            super(owner, name, desc);
            this.isFromInterface = isFromInterface;
        }

        public boolean isFromInterface() {
            return isFromInterface;
        }
    }
}
