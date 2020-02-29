package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.cache.MappingCacheManager;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.Objects;

public final class MappingKey {
    private final String mapping;
    private final MappableTypeDMO mappingType;
    private final String parentClassMapping;
    private final String parentMethodMapping;

    public MappingKey(final String mapping, final MappableTypeDMO mappingType, final String parentClassMapping, final String parentMethodMapping) {
        this.mapping = mapping;
        this.mappingType = mappingType;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MappingKey that = (MappingKey) o;

        if (!mapping.equals(that.mapping)) return false;
        if (!mappingType.equals(that.mappingType)) return false;
        if (!Objects.equals(parentClassMapping, that.parentClassMapping))
            return false;
        return Objects.equals(parentMethodMapping, that.parentMethodMapping);
    }

    @Override
    public int hashCode() {
        int result = mapping.hashCode();
        result = 31 * result + mappingType.hashCode();
        result = 31 * result + (parentClassMapping != null ? parentClassMapping.hashCode() : 0);
        result = 31 * result + (parentMethodMapping != null ? parentMethodMapping.hashCode() : 0);
        return result;
    }
}
