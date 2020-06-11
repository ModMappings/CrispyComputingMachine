package org.modmappings.crispycomputingmachine.processors.base;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalMappableType;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

public abstract class AbstractSkipIfMappingTypeNotReadyFilter implements ItemProcessor<String, String> {

    private final Function<String, String> gameVersionNameExtractor;
    private final Function<String, String> mappingTypeReleaseExtractor;
    private final String mappingTypeName;
    private final String requiredState;

    private final GameVersionRepository gameVersionRepository;
    private final ReleaseRepository releaseRepository;
    private final MappingTypeRepository mappingTypeRepository;

    public AbstractSkipIfMappingTypeNotReadyFilter(final Function<String, String> gameVersionNameExtractor, final Function<String, String> mappingTypeReleaseExtractor, final String mappingTypeName, final String requiredState, final GameVersionRepository gameVersionRepository,
                                                   final ReleaseRepository releaseRepository,
                                                   final MappingTypeRepository mappingTypeRepository) {
        this.gameVersionNameExtractor = gameVersionNameExtractor;
        this.mappingTypeReleaseExtractor = mappingTypeReleaseExtractor;
        this.mappingTypeName = mappingTypeName;
        this.requiredState = requiredState;
        this.gameVersionRepository = gameVersionRepository;
        this.releaseRepository = releaseRepository;
        this.mappingTypeRepository = mappingTypeRepository;
    }

    @Override
    public String process(@NotNull final String item) {
        final String gameVersionName = gameVersionNameExtractor.apply(item);
        final String targetMappingTypeReleaseName = mappingTypeReleaseExtractor.apply(item);

        final boolean isReleaseReady = gameVersionRepository.findAllBy(RegexUtils.createFullWordRegex(gameVersionName), null, null, Pageable.unpaged()) //Validate a game version exists.
                .flatMapIterable(Function.identity())
                .next()
                .flatMap(gameVersion -> mappingTypeRepository.findAllBy(RegexUtils.createFullWordRegex(mappingTypeName), null, false, Pageable.unpaged()) //Validate the official mapping type exists.
                        .flatMapIterable(Function.identity())
                        .next()
                        .flatMap(mappingType -> releaseRepository.findAllBy(RegexUtils.createFullWordRegex(targetMappingTypeReleaseName), gameVersion.getId(), mappingType.getId(), null, null, null, false, Pageable.unpaged()) //Now check if the official release has reached the field stage, meaning that its import completed.
                                .flatMapIterable(Function.identity())
                                .next()
                                .filter(releaseDMO -> releaseDMO.getState().equals(requiredState))))
                .blockOptional()
                .isPresent();

        if (!isReleaseReady)
            return null;

        return item;
    }
}
