package org.modmappings.crispycomputingmachine.model.mappings;

import net.minecraftforge.srgutils.IMappingFile;
import org.modmappings.crispycomputingmachine.model.srgutils.SRGUtilsWrappedMappingFile;

import java.util.Date;
import java.util.LinkedList;

public class ExternalRelease {

    private final String name;
    private final Date releasedOn;
    private final LinkedList<ExternalClass> classes;
    private final boolean isPreRelease;
    private final boolean                    isSnapshot;
    private final SRGUtilsWrappedMappingFile clientFile;
    private final SRGUtilsWrappedMappingFile serverFile;

    public ExternalRelease(
      final String name,
      final Date releasedOn,
      final LinkedList<ExternalClass> classes,
      final boolean isPreRelease,
      final boolean isSnapshot,
      final SRGUtilsWrappedMappingFile clientFile,
      final SRGUtilsWrappedMappingFile serverFile) {
        this.name = name;
        this.releasedOn = releasedOn;
        this.classes = classes;
        this.isPreRelease = isPreRelease;
        this.isSnapshot = isSnapshot;
        this.clientFile = clientFile;
        this.serverFile = serverFile;
    }

    public String getName() {
        return name;
    }

    public Date getReleasedOn() {
        return releasedOn;
    }

    public LinkedList<ExternalClass> getClasses() {
        return classes;
    }

    public boolean isPreRelease() {
        return isPreRelease;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public SRGUtilsWrappedMappingFile getClientFile()
    {
        return clientFile;
    }

    public SRGUtilsWrappedMappingFile getServerFile()
    {
        return serverFile;
    }
}
