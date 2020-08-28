package org.modmappings.crispycomputingmachine.processors.mcp;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.processors.base.AbstractSkipIfMappingTypeNotReadyFilter;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.stereotype.Component;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class MCPSkipIfMCPConfigNotReadyFilter extends AbstractSkipIfMappingTypeNotReadyFilter {

    public MCPSkipIfMCPConfigNotReadyFilter(final GameVersionRepository gameVersionRepository,
                                                final ReleaseRepository releaseRepository,
                                                final MappingTypeRepository mappingTypeRepository) {
        super(releaseName -> releaseName.split("-")[1],
                releaseName -> releaseName.split("-")[1], //MCP Config uses the MC version as versioning scheme. So just grab that.
                Constants.MCP_CONFIG_MAPPING_NAME,
                ExternalMappableType.PARAMETER.name().toLowerCase(),
                gameVersionRepository,
                releaseRepository,
                mappingTypeRepository);
    }

}
