package org.modmappings.crispycomputingmachine.model.mappings;

import org.modmappings.crispycomputingmachine.utils.MethodRef;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class ExternalVanillaMapping extends ExternalMapping {


    private final Date gameVersionReleaseDate;

    private ExternalVisibility visibility;

    private boolean isExternal;

    private List<String> superClasses;
    private Set<MethodRef> methodOverrides;

    public ExternalVanillaMapping(final String input, final String output, final ExternalMappableType mappableType, final String gameVersion, final Date gameVersionReleaseDate, final String releaseName, final String parentClassMapping, final String parentMethodMapping, final String parentMethodDescriptor, final ExternalVisibility visibility, final boolean isStatic, final String type, final String descriptor, final String signature, final boolean isExternal, final List<String> superClasses, final Set<MethodRef> methodOverrides) {
        super(input, output, mappableType, gameVersion, releaseName, parentClassMapping, parentMethodMapping, parentMethodDescriptor, type, descriptor, signature, null, isStatic);

        this.gameVersionReleaseDate = gameVersionReleaseDate;
        this.visibility = visibility;
        this.isExternal = isExternal;
        this.superClasses = superClasses;
        this.methodOverrides = methodOverrides;
    }

    public Date getGameVersionReleaseDate() {
        return gameVersionReleaseDate;
    }

    public ExternalVisibility getVisibility() {
        return visibility;
    }

    public List<String> getSuperClasses() {
        return superClasses;
    }

    public Set<MethodRef> getMethodOverrides() {
        return methodOverrides;
    }

    public boolean isExternal() {
        return isExternal;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ExternalVanillaMapping mapping = (ExternalVanillaMapping) o;

        if (isStatic() != mapping.isStatic()) return false;
        if (isExternal() != mapping.isExternal()) return false;
        if (getGameVersionReleaseDate() != null ? !getGameVersionReleaseDate().equals(mapping.getGameVersionReleaseDate()) : mapping.getGameVersionReleaseDate() != null)
            return false;
        if (getVisibility() != mapping.getVisibility()) return false;
        if (getSuperClasses() != null ? !getSuperClasses().equals(mapping.getSuperClasses()) : mapping.getSuperClasses() != null)
            return false;
        return getMethodOverrides() != null ? getMethodOverrides().equals(mapping.getMethodOverrides()) : mapping.getMethodOverrides() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getGameVersionReleaseDate() != null ? getGameVersionReleaseDate().hashCode() : 0);
        result = 31 * result + (getVisibility() != null ? getVisibility().hashCode() : 0);
        result = 31 * result + (isStatic() ? 1 : 0);
        result = 31 * result + (isExternal() ? 1 : 0);
        result = 31 * result + (getSuperClasses() != null ? getSuperClasses().hashCode() : 0);
        result = 31 * result + (getMethodOverrides() != null ? getMethodOverrides().hashCode() : 0);
        return result;
    }
}
