package org.modmappings.crispycomputingmachine.processors.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(ExternalVanillaMappingSorter.class);

    @Override
    public List<ExternalVanillaMapping> process(final List<ExternalVanillaMapping> item) throws Exception {
        final List<ExternalVanillaMapping> classes = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.CLASS).sorted(
                            Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                                .thenComparing(ExternalVanillaMapping::getOutput)
                ).collect(Collectors.toList());

        final Map<String, Integer> classIndexMap = new HashMap<>();
        int index = 0;
        for (final ExternalVanillaMapping aClass : classes) {
            classIndexMap.put(aClass.getOutput(), index++);
        }
        final List<ExternalVanillaMapping> methods = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final List<ExternalVanillaMapping> fields = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.FIELD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final List<ExternalVanillaMapping> result = new ArrayList<>(classes);
        result.addAll(methods);
        result.addAll(fields);
        //Vanilla mappings do not have parameters!;

        return result;
    }
}
