package org.modmappings.crispycomputingmachine.model.mappings;

import org.modmappings.crispycomputingmachine.utils.MethodRef;
import org.modmappings.crispycomputingmachine.utils.ParameterRef;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class ExternalVanillaMapping extends ExternalMapping {


    private final Date gameVersionReleaseDate;

    private ExternalVisibility visibility;

    private boolean isExternal;

    private List<String>   superClasses;
    private Set<MethodRef> methodOverrides;
    private Set<ParameterRef> parameterOverrides;

    public ExternalVanillaMapping(
      final String input,
      final String output,
      final ExternalMappableType mappableType,
      final String gameVersion,
      final Date gameVersionReleaseDate,
      final String releaseName,
      final String parentClassMapping,
      final String parentMethodMapping,
      final String parentMethodDescriptor,
      final ExternalVisibility visibility,
      final boolean isStatic,
      final String type,
      final String descriptor,
      final String signature,
      final int index,
      final boolean isExternal,
      final List<String> superClasses,
      final Set<MethodRef> methodOverrides,
      final Set<ParameterRef> parameterOverrides) {
        super(input, output, mappableType, gameVersion, releaseName, parentClassMapping, parentMethodMapping, parentMethodDescriptor, type, descriptor, signature, index, isStatic);

        this.gameVersionReleaseDate = gameVersionReleaseDate;
        this.visibility = visibility;
        this.isExternal = isExternal;
        this.superClasses = superClasses;
        this.methodOverrides = methodOverrides;
        this.parameterOverrides = parameterOverrides;

        if (mappableType == ExternalMappableType.PARAMETER && (parentMethodDescriptor == null || parentMethodMapping == null))
            System.out.println("Found it");
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

    public Set<ParameterRef> getParameterOverrides()
    {
        return parameterOverrides;
    }

    public boolean isExternal() {
        return isExternal;
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
        if (!super.equals(o))
        {
            return false;
        }

        final ExternalVanillaMapping that = (ExternalVanillaMapping) o;

        if (isExternal() != that.isExternal())
        {
            return false;
        }
        if (getGameVersionReleaseDate() != null ? !getGameVersionReleaseDate().equals(that.getGameVersionReleaseDate()) : that.getGameVersionReleaseDate() != null)
        {
            return false;
        }
        if (getVisibility() != that.getVisibility())
        {
            return false;
        }
        if (getSuperClasses() != null ? !getSuperClasses().equals(that.getSuperClasses()) : that.getSuperClasses() != null)
        {
            return false;
        }
        if (getMethodOverrides() != null ? !getMethodOverrides().equals(that.getMethodOverrides()) : that.getMethodOverrides() != null)
        {
            return false;
        }
        return getParameterOverrides() != null ? getParameterOverrides().equals(that.getParameterOverrides()) : that.getParameterOverrides() == null;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (getGameVersionReleaseDate() != null ? getGameVersionReleaseDate().hashCode() : 0);
        result = 31 * result + (getVisibility() != null ? getVisibility().hashCode() : 0);
        result = 31 * result + (isExternal() ? 1 : 0);
        result = 31 * result + (getSuperClasses() != null ? getSuperClasses().hashCode() : 0);
        result = 31 * result + (getMethodOverrides() != null ? getMethodOverrides().hashCode() : 0);
        result = 31 * result + (getParameterOverrides() != null ? getParameterOverrides().hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "ExternalVanillaMapping{" +
                 "gameVersionReleaseDate=" + gameVersionReleaseDate +
                 ", visibility=" + visibility +
                 ", isExternal=" + isExternal +
                 ", superClasses=" + superClasses +
                 ", methodOverrides=" + methodOverrides +
                 ", parameterOverrides=" + parameterOverrides +
                 "} " + super.toString();
    }
}
