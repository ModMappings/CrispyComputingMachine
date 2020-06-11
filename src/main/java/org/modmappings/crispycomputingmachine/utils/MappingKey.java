package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;

import java.util.Objects;

public final class MappingKey {
    private final String mapping;
    private final MappableTypeDMO mappingType;
    private final String parentClassMapping;
    private final String parentMethodMapping;
    private final String parentMethodDescriptor;
    private final String type;
    private final String descriptor;

    public MappingKey(final String mapping, final MappableTypeDMO mappingType, final String parentClassMapping, final String parentMethodMapping, final String parentMethodDescriptor, final String type, final String descriptor) {
        this.mapping = mapping;
        this.mappingType = mappingType;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.parentMethodDescriptor = parentMethodDescriptor;
        this.type = type;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MappingKey that = (MappingKey) o;

        if (getMapping() != null ? !getMapping().equals(that.getMapping()) : that.getMapping() != null) return false;
        if (getMappingType() != that.getMappingType()) return false;
        if (getParentClassMapping() != null ? !getParentClassMapping().equals(that.getParentClassMapping()) : that.getParentClassMapping() != null) return false;
        if (getParentMethodMapping() != null ? !getParentMethodMapping().equals(that.getParentMethodMapping()) : that.getParentMethodMapping() != null) return false;
        if (getParentMethodDescriptor() != null ? !getParentMethodDescriptor().equals(that.getParentMethodDescriptor()) : that.getParentMethodDescriptor() != null) return false;
        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) return false;
        return getDescriptor() != null ? getDescriptor().equals(that.getDescriptor()) : that.getDescriptor() == null;
    }

    @Override
    public int hashCode() {
        int result = getMapping() != null ? getMapping().hashCode() : 0;
        result = 31 * result + (getMappingType() != null ? getMappingType().hashCode() : 0);
        result = 31 * result + (getParentClassMapping() != null ? getParentClassMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodMapping() != null ? getParentMethodMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodDescriptor() != null ? getParentMethodDescriptor().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getDescriptor() != null ? getDescriptor().hashCode() : 0);
        return result;
    }

    public String getMapping() {
        return mapping;
    }

    public MappableTypeDMO getMappingType() {
        return mappingType;
    }

    public String getParentClassMapping() {
        return parentClassMapping;
    }

    public String getParentMethodMapping() {
        return parentMethodMapping;
    }

    public String getParentMethodDescriptor() {
        return parentMethodDescriptor;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }
}
