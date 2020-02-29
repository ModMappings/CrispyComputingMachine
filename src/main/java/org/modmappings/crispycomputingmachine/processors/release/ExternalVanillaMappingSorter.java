package org.modmappings.crispycomputingmachine.processors.release;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.springframework.batch.item.ItemProcessor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalVanillaMappingSorter implements ItemProcessor<List<ExternalVanillaMapping>, List<ExternalVanillaMapping>> {
    @Override
    public List<ExternalVanillaMapping> process(final List<ExternalVanillaMapping> item) throws Exception {
        return item.stream().sorted((left, right) -> {
            //First game versions come first.
            if (!left.getGameVersionReleaseDate().equals(right.getGameVersionReleaseDate()))
                return left.getGameVersionReleaseDate().compareTo(right.getGameVersionReleaseDate());

            //Class first, then method, fields, and parameters.
            if (!left.getMappableType().equals(right.getMappableType()))
                return left.getMappableType().ordinal() - right.getMappableType().ordinal();

            final long leftSubClassDepth = left.getOutput().chars().filter(ch -> ch == '$').count();
            final long rightSubClassDepth = right.getOutput().chars().filter(ch -> ch == '$').count();
            if (leftSubClassDepth != rightSubClassDepth)
                return (int) (leftSubClassDepth - rightSubClassDepth);

            //Parent classes and method before their children.
            final Comparator<String> parentComparator = Comparator.nullsFirst(Comparator.naturalOrder());
            final int parentClassComparison = parentComparator.compare(left.getParentClassMapping(), right.getParentClassMapping());
            if (parentClassComparison != 0)
                return parentClassComparison;

            final int parentMethodComparison = parentComparator.compare(left.getParentMethodMapping(), right.getParentMethodMapping());
            if (parentMethodComparison != 0)
                return parentMethodComparison;

            //Then just do the natural order of the output for readability.
            return left.getOutput().compareTo(right.getOutput());
        }).collect(Collectors.toList());
    }
}
