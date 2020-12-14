package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.processors.base.AbstractSkipIfMappingTypeNotReadyFilter;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class MCPConfigSkipIfOfficialNotReadyFilter extends AbstractSkipIfMappingTypeNotReadyFilter {

    public MCPConfigSkipIfOfficialNotReadyFilter(final GameVersionRepository gameVersionRepository,
                                                 final ReleaseRepository releaseRepository,
                                                 final MappingTypeRepository mappingTypeRepository) {
        super(
                releaseName -> releaseName.split("-")[0],
                releaseName -> releaseName.split("-")[0],
                Constants.OFFICIAL_MAPPING_NAME,
                ExternalMappableType.PARAMETER.name().toLowerCase(),
                gameVersionRepository,
                releaseRepository,
                mappingTypeRepository
        );
    }

}
