package org.modmappings.crispycomputingmachine.processors.base;

import org.jetbrains.annotations.NotNull;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

public abstract class AbstractSkipIfReleaseExistsFilter implements ItemProcessor<String, String> {

    private final Function<String, String> gameVersionNameExtractor;
    private final String mappingTypeName;

    private final GameVersionRepository gameVersionRepository;
    private final ReleaseRepository releaseRepository;
    private final MappingTypeRepository mappingTypeRepository;

    public AbstractSkipIfReleaseExistsFilter(final Function<String, String> gameVersionNameExtractor, final String mappingTypeName, final GameVersionRepository gameVersionRepository,
                                             final ReleaseRepository releaseRepository,
                                             final MappingTypeRepository mappingTypeRepository) {
        this.gameVersionNameExtractor = gameVersionNameExtractor;
        this.mappingTypeName = mappingTypeName;
        this.gameVersionRepository = gameVersionRepository;
        this.releaseRepository = releaseRepository;
        this.mappingTypeRepository = mappingTypeRepository;
    }

    @Override
    public String process(@NotNull final String item) {
        final String gameVersionName = gameVersionNameExtractor.apply(item);

        final boolean isAlreadyImported = gameVersionRepository.findAllBy(RegexUtils.createFullWordRegex(gameVersionName), null, null, Pageable.unpaged()) //Validate a game version exists.
                .flatMapIterable(Function.identity())
                .next()
                .flatMap(gameVersion -> mappingTypeRepository.findAllBy(RegexUtils.createFullWordRegex(mappingTypeName), null, false, Pageable.unpaged()) //Validate the official mapping type exists.
                        .flatMapIterable(Function.identity())
                        .next()
                        .flatMap(mappingType -> releaseRepository.findAllBy(RegexUtils.createFullWordRegex(item), gameVersion.getId(), mappingType.getId(), null, null, null, false, Pageable.unpaged()) //Now check if the official release has reached the field stage, meaning that its import completed.
                                .flatMapIterable(Function.identity())
                                .next()))
                .blockOptional()
                .isPresent();

        if (isAlreadyImported)
            return null;

        return item;
    }
}
