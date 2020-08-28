package org.modmappings.crispycomputingmachine.processors.mcp;

import org.modmappings.crispycomputingmachine.processors.base.AbstractSkipIfReleaseExistsFilter;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.stereotype.Component;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class MCPSkipIfReleaseExistsFilter extends AbstractSkipIfReleaseExistsFilter {

    public MCPSkipIfReleaseExistsFilter(final GameVersionRepository gameVersionRepository,
                                         final ReleaseRepository releaseRepository,
                                         final MappingTypeRepository mappingTypeRepository) {
        super(releaseName -> releaseName.split("-")[0],
                Constants.MCP_MAPPING_NAME,
                gameVersionRepository,
                releaseRepository,
                mappingTypeRepository);
    }

}
