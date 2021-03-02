package org.modmappings.crispycomputingmachine.utils;

import net.minecraftforge.srgutils.IMappingFile;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalVanillaMapping;
import org.modmappings.crispycomputingmachine.model.mappingtoy.MappingToyJarMetaData;
import org.modmappings.crispycomputingmachine.model.srgutils.SRGUtilsWrappedMappingFile;

public class MethodRef implements Comparable<MethodRef> {
    private final String owner;
    private final String name;
    private final String desc;

    public MethodRef(ExternalVanillaMapping evm) {
        this(evm.getParentClassMapping(), evm.getOutput(), evm.getDescriptor());
    }

    public MethodRef(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public MethodRef(MappingToyJarMetaData.ClassInfo classInfo, MappingToyJarMetaData.ClassInfo.MethodInfo methodInfo)
    {
        this.owner = classInfo.getName();
        this.name = methodInfo.getName();
        this.desc = methodInfo.getDesc();
    }

    @Override
    public String toString() {
        return this.owner + '/' + this.name + this.desc;
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

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public MethodRef remap(final SRGUtilsWrappedMappingFile mappingFile)
    {
        final IMappingFile.IClass classOfOwner = mappingFile.findClassFromName(owner);

        if (classOfOwner == null)
            return this;

        return new MethodRef(
                classOfOwner.getMapped(),
                classOfOwner.remapMethod(name, desc),
                desc
        );
    }

    @Override
    public int compareTo(MethodRef o) {
        return compare(owner.compareTo(o.owner), compare(name.compareTo(o.name), desc.compareTo(o.desc)));
    }
}
