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
    private final boolean onlyMappingBased;

    public MappingKey(final String mapping, final MappableTypeDMO mappingType, final String parentClassMapping, final String parentMethodMapping, final String parentMethodDescriptor, final String type, final String descriptor, final boolean onlyMappingBased) {
        this.mapping = mapping;
        this.mappingType = mappingType;
        this.parentClassMapping = parentClassMapping;
        this.parentMethodMapping = parentMethodMapping;
        this.parentMethodDescriptor = parentMethodDescriptor;
        this.type = type;
        this.descriptor = descriptor;
        this.onlyMappingBased = onlyMappingBased;
    }

    @Override
    public int hashCode()
    {
        if (this.isOnlyMappingBased())
            return mappingOnlyHashcode();

        return fullHashcode();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final MappingKey that = (MappingKey) o;

        if (that.isOnlyMappingBased() != this.isOnlyMappingBased())
            return false;

        if (this.isOnlyMappingBased())
            return mappingOnlyEquals(that);

        return fullEquals(that);
    }

    public boolean mappingOnlyEquals(final MappingKey that)
    {
        return getMapping().equals(that.getMapping());
    }

    public int mappingOnlyHashcode()
    {
        return getMapping().hashCode();
    }

    public boolean fullEquals(final MappingKey that)
    {

        if (!getMapping().equals(that.getMapping()))
        {
            return false;
        }
        if (getMappingType() != that.getMappingType())
        {
            return false;
        }
        if (getParentClassMapping() != null ? !getParentClassMapping().equals(that.getParentClassMapping()) : that.getParentClassMapping() != null)
        {
            return false;
        }
        if (getParentMethodMapping() != null ? !getParentMethodMapping().equals(that.getParentMethodMapping()) : that.getParentMethodMapping() != null)
        {
            return false;
        }
        if (getParentMethodDescriptor() != null ? !getParentMethodDescriptor().equals(that.getParentMethodDescriptor()) : that.getParentMethodDescriptor() != null)
        {
            return false;
        }
        return getDescriptor() != null ? getDescriptor().equals(that.getDescriptor()) : that.getDescriptor() == null;
    }

    public int fullHashcode()
    {
        int result = getMapping().hashCode();
        result = 31 * result + getMappingType().hashCode();
        result = 31 * result + (getParentClassMapping() != null ? getParentClassMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodMapping() != null ? getParentMethodMapping().hashCode() : 0);
        result = 31 * result + (getParentMethodDescriptor() != null ? getParentMethodDescriptor().hashCode() : 0);
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

    private boolean isOnlyMappingBased()
    {
        return onlyMappingBased;
    }
}
