package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.Objects;

public final class MappingKey {
    private final String mapping;
    private final MappableTypeDMO mappingType;
    private final String parentClassMapping;
    private final String parentMethodMapping;
    private final String type;
    private final String descriptor;

    public MappingKey(final String mapping, final MappableTypeDMO mappingType, final String parentClassMapping, final String parentMethodMapping, final String type, final String descriptor) {
        this.mapping = mapping;
        this.mappingType = mappingType;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.type = type;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MappingKey that = (MappingKey) o;

        if (!mapping.equals(that.mapping)) return false;
        if (mappingType != that.mappingType) return false;
        if (!Objects.equals(parentClassMapping, that.parentClassMapping))
            return false;
        if (!Objects.equals(parentMethodMapping, that.parentMethodMapping))
            return false;
        if (!Objects.equals(type, that.type)) return false;
        return Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        if (mapping == null)
            throw new NullPointerException();

        int result = mapping.hashCode();
        result = 31 * result + mappingType.hashCode();
        result = 31 * result + (parentClassMapping != null ? parentClassMapping.hashCode() : 0);
        result = 31 * result + (parentMethodMapping != null ? parentMethodMapping.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
        return result;
    }
}
