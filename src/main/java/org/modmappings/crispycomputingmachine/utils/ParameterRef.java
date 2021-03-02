package org.modmappings.crispycomputingmachine.utils;

import net.minecraftforge.srgutils.IMappingFile;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.model.srgutils.SRGUtilsWrappedMappingFile;

public class ParameterRef implements Comparable<ParameterRef>
{
    private final String owner;
    private final String method;
    private final String desc;
    private final int index;
    private final String type;

    public ParameterRef(final ExternalVanillaMapping mapping) {
        this(mapping.getParentClassMapping(),
          mapping.getParentMethodMapping(),
          mapping.getParentMethodDescriptor(),
          mapping.getIndex(),
          mapping.getType());
    }

    public ParameterRef(final MethodRef parent, final int index, final String type) {
        this(parent.getOwner(), parent.getName(), parent.getDesc(), index, type);
    }

    public ParameterRef(final String owner, final String method, final String desc, final int index, final String type) {
        this.owner = owner;
        this.method = method;
        this.desc = desc;
        this.index = index;
        this.type = type;
    }

    @Override
    public String toString() {
        return this.owner + '#' + this.method + this.desc + "/" + this.method + "_" + this.index + this.type;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MethodRef && o.toString().equals(toString());
    }

    private int compare(int a, int b) {
        return a != 0 ? a : b;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getMethod()
    {
        return method;
    }

    public String getDesc()
    {
        return desc;
    }

    public int getIndex()
    {
        return index;
    }

    public String getType()
    {
        return type;
    }

    public ParameterRef remap(final SRGUtilsWrappedMappingFile mappingFile)
    {
        final IMappingFile.IClass classOfOwner = mappingFile.findClassFromName(owner);

        if (classOfOwner == null)
            return this;

        final String remappedMethod = classOfOwner.remapMethod(method, desc);
        return new ParameterRef(
          classOfOwner.getMapped(),
          remappedMethod,
          desc,
          index,
          type
        );
    }

    @Override
    public int compareTo(ParameterRef o) {
        return compare(owner.compareTo(o.owner), compare(method.compareTo(o.method), compare(desc.compareTo(o.desc), compare(index-o.index, type.compareTo(o.type)))));
    }
}
