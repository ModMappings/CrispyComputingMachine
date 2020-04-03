package org.modmappings.crispycomputingmachine.processors.mcpconfig;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ExistingMCPConfigMappingMinecraftVersionFilter implements ItemProcessor<String, String> {

    private final GameVersionRepository gameVersionRepository;
    private final ReleaseRepository releaseRepository;
    private final MappingTypeRepository mappingTypeRepository;

    public ExistingMCPConfigMappingMinecraftVersionFilter(final GameVersionRepository gameVersionRepository,
                                                          final ReleaseRepository releaseRepository,
                                                          final MappingTypeRepository mappingTypeRepository) {
        this.gameVersionRepository = gameVersionRepository;
        this.releaseRepository = releaseRepository;
        this.mappingTypeRepository = mappingTypeRepository;
    }

    @Override
    public String process(final String item) {
        final String gameVersionName = item.split("-")[0];

        final boolean isVanillaReady = gameVersionRepository.findAllBy(RegexUtils.createFullWordRegex(gameVersionName.replace(".", "\\.")), null, null, Pageable.unpaged()) //Validate a game version exists.
                .flatMapIterable(Function.identity())
                .next()
                .flatMap(gameVersion -> mappingTypeRepository.findAllBy(RegexUtils.createFullWordRegex(Constants.OFFICIAL_MAPPING_NAME), null, false, Pageable.unpaged()) //Validate the official mapping type exists.
                        .flatMapIterable(Function.identity())
                        .next()
                        .flatMap(mappingType -> releaseRepository.findAllBy(RegexUtils.createFullWordRegex(item.replace(".", "\\.")), gameVersion.getId(), mappingType.getId(), null, null, null, false, Pageable.unpaged()) //Now check if the official release has reached the field stage, meaning that its import completed.
                                .flatMapIterable(Function.identity())
                                .next()
                                .filter(releaseDMO -> releaseDMO.getState().equals(ExternalMappableType.FIELD.toString().toLowerCase()))))
                .blockOptional()
                .isPresent();

        if (!isVanillaReady)
            return null;

        final boolean isAlreadyImported = gameVersionRepository.findAllBy(RegexUtils.createFullWordRegex(gameVersionName.replace(".", "\\.")), null, null, Pageable.unpaged()) //Validate a game version exists.
                .flatMapIterable(Function.identity())
                .next()
                .flatMap(gameVersion -> mappingTypeRepository.findAllBy(RegexUtils.createFullWordRegex(Constants.MCP_CONFIG_MAPPING_NAME), null, false, Pageable.unpaged()) //Validate the official mapping type exists.
                        .flatMapIterable(Function.identity())
                        .next()
                        .flatMap(mappingType -> releaseRepository.findAllBy(RegexUtils.createFullWordRegex(item.replace(".", "\\.")), gameVersion.getId(), mappingType.getId(), null, null, null, false, Pageable.unpaged()) //Now check if the official release has reached the field stage, meaning that its import completed.
                                .flatMapIterable(Function.identity())
                                .next()))
                .blockOptional()
                .isPresent();

        if (isAlreadyImported)
            return null;

        return item;
    }
}
