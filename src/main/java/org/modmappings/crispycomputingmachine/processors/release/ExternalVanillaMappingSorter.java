package org.modmappings.crispycomputingmachine.processors.release;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalVanillaMappingSorter implements ItemProcessor<List<ExternalVanillaMapping>, List<ExternalVanillaMapping>> {

    @Override
    public List<ExternalVanillaMapping> process(final List<ExternalVanillaMapping> item) throws Exception {
        final Map<ExternalVanillaMapping, Set<ExternalVanillaMapping>> inheritanceOrOverrideData = calculateInheritanceOrOverrideMap(
                item
        );

        final List<ExternalVanillaMapping> classes = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.CLASS)
                .collect(Collectors.toList());
        classes.sort(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                .thenComparing((left, right) -> {
                    if((left.getOutput().contains("com/mojang/blaze3d/audio/OggAudioStream") && right.getOutput().contains("net/minecraft/client/sounds/AudioStream")) ||
                            (right.getOutput().contains("com/mojang/blaze3d/audio/OggAudioStream") && left.getOutput().contains("net/minecraft/client/sounds/AudioStream")))
                            System.out.println("Found them!");

                    if (left.getMappableType() == ExternalMappableType.CLASS &&
                            right.getMappableType() == ExternalMappableType.CLASS) {
                        if (inheritanceOrOverrideData.get(right).contains(left))
                            return -1;

                        //If right is super class, or overriden by, left then rights needs to go first.
                        if (inheritanceOrOverrideData.get(left).contains(right))
                            return 1;
                    }

                    return 0;
                })
                .thenComparing(evm -> evm.getOutput().chars().filter(ch -> ch == '$').count())
                .thenComparing(ExternalVanillaMapping::getParentClassMapping, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ExternalVanillaMapping::getParentMethodMapping, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ExternalVanillaMapping::getOutput)
        );

        final Map<String, Integer> classIndexMap = new HashMap<>();
        int index = 0;
        for (final ExternalVanillaMapping aClass : classes) {
            classIndexMap.put(aClass.getOutput(), index++);
        }
        final List<ExternalVanillaMapping> methods = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                        .thenComparing(ExternalVanillaMapping::getParentMethodMapping, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final List<ExternalVanillaMapping> fields = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.FIELD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder()).thenComparing(ExternalVanillaMapping::getParentMethodMapping, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final List<ExternalVanillaMapping> result = new ArrayList<>(classes);
        result.addAll(methods);
        result.addAll(fields);
        //Vanilla mappings do not have parameters!;

        return result;
    }

    private Map<ExternalVanillaMapping, Set<ExternalVanillaMapping>> calculateInheritanceOrOverrideMap(List<ExternalVanillaMapping> externalVanillaMappings) {
        final Map<String, ExternalVanillaMapping> lookupMap = externalVanillaMappings.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.CLASS)
                .collect(Collectors.toMap(ExternalVanillaMapping::getOutput, Function.identity()));
        final Map<ExternalVanillaMapping, Set<ExternalVanillaMapping>> result = new HashMap<>();
        externalVanillaMappings.forEach(evm -> {
            determineInheritanceOf(evm, result, lookupMap);
        });

        return result;
    }

    private Set<ExternalVanillaMapping> determineInheritanceOf(
            final ExternalVanillaMapping evm,
            final Map<ExternalVanillaMapping, Set<ExternalVanillaMapping>> targetMap,
            final Map<String, ExternalVanillaMapping> lookupMap
    ) {
        if (evm.getSuperClasses().isEmpty()) {
            targetMap.put(evm, Collections.emptySet());
            return Collections.emptySet();
        }

        if (targetMap.containsKey(evm))
            return targetMap.get(evm);

        final Set<ExternalVanillaMapping> parentClasses = new HashSet<>();
        evm.getSuperClasses().forEach(superMapping -> {
            final ExternalVanillaMapping superEvm = lookupMap.get(superMapping);
            final Set<ExternalVanillaMapping> superEvms = determineInheritanceOf(
                    superEvm,
                    targetMap,
                    lookupMap
            );
            parentClasses.addAll(superEvms);
            parentClasses.add(superEvm);
        });

        targetMap.put(evm, parentClasses);
        return parentClasses;
    }
}
