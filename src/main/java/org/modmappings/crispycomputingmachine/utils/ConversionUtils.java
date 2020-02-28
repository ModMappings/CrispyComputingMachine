package org.modmappings.crispycomputingmachine.utils;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalVisibility;
import org.modmappings.mmms.repository.model.mapping.mappable.VisibilityDMO;

public final class ConversionUtils {

    private ConversionUtils() {
        throw new IllegalStateException("Tried to initialize: ConversionUtils but this is a Utility class.");
    }

    public static VisibilityDMO toVisibilityDMO(ExternalVisibility externalVisibility)
    {
        if (externalVisibility == null)
            return null;

        return VisibilityDMO.valueOf(externalVisibility.name());
    }
}
