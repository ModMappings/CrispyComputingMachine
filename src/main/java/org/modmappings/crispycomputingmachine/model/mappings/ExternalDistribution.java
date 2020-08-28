package org.modmappings.crispycomputingmachine.model.mappings;

import org.modmappings.mmms.repository.model.mapping.mappings.DistributionDMO;

public enum ExternalDistribution
{
    CLIENT(DistributionDMO.CLIENT_ONLY),
    SERVER(DistributionDMO.SERVER_ONLY),
    COMMON(DistributionDMO.BOTH),
    UNKNOWN(DistributionDMO.UNKNOWN);

    private final DistributionDMO dmo;

    ExternalDistribution(final DistributionDMO dmo) {this.dmo = dmo;}

    public DistributionDMO getDmo()
    {
        return dmo;
    }
}
