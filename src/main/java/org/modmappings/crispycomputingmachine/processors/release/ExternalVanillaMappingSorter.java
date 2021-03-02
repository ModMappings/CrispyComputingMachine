package org.modmappings.crispycomputingmachine.processors.release;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.utils.MethodRef;
import org.modmappings.crispycomputingmachine.utils.ParameterRef;
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

        final Multimap<MethodRef, MethodRef> directMethodDependencies = HashMultimap.create();
        item.stream()
          .filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
          .forEach(evm -> directMethodDependencies.putAll(new MethodRef(evm), evm.getMethodOverrides()));

        final Multimap<MethodRef, MethodRef> fullMethodDependencies = HashMultimap.create();
        item.stream()
          .filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
          .map(MethodRef::new)
          .forEach(ref -> {
              final Queue<MethodRef> queue = new ArrayDeque<>();
              queue.offer(ref);

              final List<MethodRef> result = new ArrayList<>();
              while(!queue.isEmpty())
              {
                  final MethodRef target = queue.poll();
                  if (target != ref)
                      result.add(target);

                  directMethodDependencies.get(target)
                    .stream()
                    .filter(next -> next != ref && next != target && !result.contains(next))
                    .forEach(queue::offer);
              }

              fullMethodDependencies.putAll(ref, result);
          });



        final List<ExternalVanillaMapping> methods = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.METHOD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final List<ExternalVanillaMapping> fields = item.stream()
                .filter(evm -> evm.getMappableType() == ExternalMappableType.FIELD)
                .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                        .thenComparing(MethodRef::new, (l, r) -> {
                            final Collection<MethodRef> lDeps = fullMethodDependencies.get(l);
                            final Collection<MethodRef> rDeps = fullMethodDependencies.get(r);

                            if (lDeps.contains(r) && !rDeps.contains(l))
                                return 1;

                            if (!lDeps.contains(r) && rDeps.contains(l))
                                return -1;

                            return 0;
                        })
                        .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                        .thenComparing(ExternalVanillaMapping::getOutput))
                .collect(Collectors.toList());

        final Multimap<ParameterRef, ParameterRef> directParameterDependencies = HashMultimap.create();
        item.stream()
          .filter(evm -> evm.getMappableType() == ExternalMappableType.PARAMETER)
          .forEach(evm -> directParameterDependencies.putAll(new ParameterRef(evm), evm.getParameterOverrides()));

        final Multimap<ParameterRef, ParameterRef> fullParameterDependencies = HashMultimap.create();
        item.stream()
          .filter(evm -> evm.getMappableType() == ExternalMappableType.PARAMETER)
          .map(ParameterRef::new)
          .forEach(ref -> {
              final Queue<ParameterRef> queue = new ArrayDeque<>();
              queue.offer(ref);
              
              final List<ParameterRef> result = new ArrayList<>();
              while(!queue.isEmpty())
              {
                  final ParameterRef target = queue.poll();
                  if (target != ref)
                      result.add(target);
                  
                  directParameterDependencies.get(target)
                    .stream()
                    .filter(next -> next != ref && next != target && !result.contains(next))
                    .forEach(queue::offer);
              }
              
              fullParameterDependencies.putAll(ref, result);
          });

        final List<ExternalVanillaMapping> parameters = item.stream()
                                                                    .filter(evm -> evm.getMappableType() == ExternalMappableType.PARAMETER)
                                                                    .sorted(Comparator.comparing(ExternalVanillaMapping::getGameVersionReleaseDate)
                                                                                            .thenComparing(ParameterRef::new, (l, r) -> {
                                                                                                final Collection<ParameterRef> lDeps = fullParameterDependencies.get(l);
                                                                                                final Collection<ParameterRef> rDeps = fullParameterDependencies.get(r);
                                                                                                
                                                                                                if (lDeps.contains(r) && !rDeps.contains(l))
                                                                                                    return 1;
                                                                                                
                                                                                                if (!lDeps.contains(r) && rDeps.contains(l))
                                                                                                    return -1;
                                                                                                
                                                                                                return 0;
                                                                                            })
                                                                                            .thenComparing(evm -> classIndexMap.get(evm.getParentClassMapping()), Comparator.naturalOrder())
                                                                                            .thenComparing(ExternalMapping::getParentMethodMapping, Comparator.nullsLast(Comparator.naturalOrder()))
                                                                                            .thenComparing(ExternalMapping::getParentMethodDescriptor, Comparator.nullsLast(Comparator.naturalOrder()))
                                                                                            .thenComparing(ExternalVanillaMapping::getOutput))
                                                                    .collect(Collectors.toList());

        final List<ExternalVanillaMapping> result = new ArrayList<>(classes);
        result.addAll(methods);
        result.addAll(fields);
        result.addAll(parameters);

        return result;
    }
}
