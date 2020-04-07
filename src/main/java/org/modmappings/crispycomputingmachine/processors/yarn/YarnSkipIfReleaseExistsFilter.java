package org.modmappings.crispycomputingmachine.processors.yarn;

import org.modmappings.crispycomputingmachine.processors.base.AbstractSkipIfReleaseExistsFilter;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class YarnSkipIfReleaseExistsFilter extends AbstractSkipIfReleaseExistsFilter {

    public YarnSkipIfReleaseExistsFilter(final GameVersionRepository gameVersionRepository,
                                         final ReleaseRepository releaseRepository,
                                         final MappingTypeRepository mappingTypeRepository) {
        super(Function.identity(),
                Constants.YARN_MAPPING_NAME,
                gameVersionRepository,
                releaseRepository,
                mappingTypeRepository);
    }

}
