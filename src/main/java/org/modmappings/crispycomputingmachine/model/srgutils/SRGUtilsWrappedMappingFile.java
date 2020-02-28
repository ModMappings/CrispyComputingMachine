package org.modmappings.crispycomputingmachine.model.srgutils;

import net.minecraftforge.srgutils.IMappingFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SRGUtilsWrappedMappingFile {
    private static final Logger LOGGER = LogManager.getLogger(SRGUtilsWrappedMappingFile.class);

    private final Map<String, IMappingFile.IClass> classCache = new ConcurrentHashMap<>();

    private final IMappingFile mappingFile;

    public SRGUtilsWrappedMappingFile(IMappingFile mappingFile) {
        this.mappingFile = mappingFile;
    }

    public String remapClass(final String obfName) {
        return this.mappingFile.remapClass(obfName);
    }

    public IMappingFile.IClass findClassFromName(final String obfClassName) {
        IMappingFile.IClass ret = classCache.get(obfClassName);
        if (ret == null) {
            ret = this.mappingFile.getClass(obfClassName);
            if (ret != null)
                classCache.put(obfClassName, ret);
            else
                LOGGER.warn("Could not find class for name: " + obfClassName);
        }

        return ret;
    }

    public Map<String, IMappingFile.IClass> getClassCache() {
        return classCache;
    }
}