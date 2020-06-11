package org.modmappings.crispycomputingmachine.processors.yarn;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.processors.base.AbstractSkipIfMappingTypeNotReadyFilter;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class YarnSkipIfIntermediaryNotReadyFilter extends AbstractSkipIfMappingTypeNotReadyFilter {

    public YarnSkipIfIntermediaryNotReadyFilter(final GameVersionRepository gameVersionRepository,
                                                final ReleaseRepository releaseRepository,
                                                final MappingTypeRepository mappingTypeRepository) {
        super(releaseName -> releaseName.split("\\+")[0],
                releaseName -> releaseName.split("\\+")[0],
                Constants.INTERMEDIARY_MAPPING_NAME,
                ExternalMappableType.PARAMETER.name().toLowerCase(),
                gameVersionRepository,
                releaseRepository,
                mappingTypeRepository);
    }

}
