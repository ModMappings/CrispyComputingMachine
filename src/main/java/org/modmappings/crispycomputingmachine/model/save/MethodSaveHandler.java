package org.modmappings.crispycomputingmachine.model.save;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMethod;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.ConversionUtils;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.DistributionDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.modmappings.mmms.repository.repositories.core.releases.components.ReleaseComponentRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.mappable.MappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.versionedmappables.VersionedMappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappings.mapping.MappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;

public class MethodSaveHandler {

    private final UUID mappingTypeId;
    private final ReleaseDMO release;
    private final ExternalMethod externalMethod;
    private final VersionedMappableDMO classVersionedMappable;

    private MappableDMO mappable;
    private VersionedMappableDMO versionedMappable;
    private MappingDMO mapping;

    private MethodSaveHandler(final UUID mappingTypeId, final ReleaseDMO release, final ExternalMethod externalMethod, final VersionedMappableDMO classVersionedMappable) {
        this.mappingTypeId = mappingTypeId;
        this.release = release;
        this.externalMethod = externalMethod;
        this.classVersionedMappable = classVersionedMappable;
    }

    public static Mono<MethodSaveHandler> createAndRunSave(
            final UUID mappingTypeId,
            final ReleaseDMO release,
            final ExternalMethod externalMethod,
            final VersionedMappableDMO classVersionedMappable,
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository,
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Mono.just(
                new MethodSaveHandler(mappingTypeId, release, externalMethod, classVersionedMappable)
        )
                .flatMap(csv -> csv.determineOrCreateAndSaveMappable(
                        mappingRepository,
                        versionedMappableRepository,
                        mappableRepository
                        )
                )
                .flatMap(csv -> csv.createAndSaveVersionedMappable(
                        versionedMappableRepository
                        )
                )
                .flatMap(csv -> csv.createMapping(
                        mappingRepository
                        )
                )
                .flatMap(csv -> csv.createReleaseComponent(
                        releaseComponentRepository
                        )
                );
    }

    private Mono<MethodSaveHandler> determineOrCreateAndSaveMappable(
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository
    )
    {
        return mappableRepository.findById(classVersionedMappable.getMappableId())
                .flatMap(classMappable -> versionedMappableRepository.findAllForMappable(
                        classMappable.getId(),
                        Pageable.unpaged()
                    )
                )
                .flatMapIterable(Function.identity())
                .filter(vmCandidate -> !vmCandidate.getId().equals(classVersionedMappable.getId()))
                .flatMap(previousClassVm -> versionedMappableRepository.findAllFor(
                        null,
                        MappableTypeDMO.METHOD,
                        previousClassVm.getId(),
                        null,
                        null,
                        null,
                        null,
                        Pageable.unpaged()
                    )
                    .flatMapIterable(Function.identity())
                )
                .filter(dmo -> dmo.getDescriptor().equals(externalMethod.getSignature()))
                .flatMap(vmd -> mappingRepository.findAllOrLatestFor(
                        true,
                        vmd.getId(),
                        null,
                        MappableTypeDMO.METHOD,
                        null,
                        RegexUtils.createFullWordRegex(externalMethod.getOutput()),
                        mappingTypeId,
                        null,
                        null,
                        false,
                        PageRequest.of(0,1)
                    )
                    .flatMapIterable(Function.identity())
                    .next()
                )
                .sort(Comparator.comparing(MappingDMO::getCreatedOn).reversed())
                .next()
                .flatMap(sameMethodInPreviousVersionMapping -> versionedMappableRepository.findById(sameMethodInPreviousVersionMapping.getVersionedMappableId()))
                .flatMap(sameMethodInPreviousVersionVM -> mappableRepository.findById(sameMethodInPreviousVersionVM.getMappableId()))
                .switchIfEmpty(Mono.just(
                        new MappableDMO(Constants.SYSTEM_ID, MappableTypeDMO.METHOD)
                        )
                )
                .flatMap(mappableRepository::save)
                .doOnNext(this::setMappable)
                .then(Mono.just(this));
    }

    private Mono<MethodSaveHandler> createAndSaveVersionedMappable(
            final VersionedMappableRepository versionedMappableRepository
    )
    {
        return Mono.just(
                VersionedMappableDMO.newMethod(
                        Constants.SYSTEM_ID,
                        release.getGameVersionId(),
                        mappable.getId(),
                        ConversionUtils.toVisibilityDMO(externalMethod.getVisibility()),
                        externalMethod.isStatic(),
                        classVersionedMappable.getId(),
                        externalMethod.getSignature()
                )
        )
                .flatMap(versionedMappableRepository::save)
                .doOnNext(this::setVersionedMappable)
                .then(Mono.just(this));
    }

    private Mono<MethodSaveHandler> createMapping(
            final MappingRepository mappingRepository
    )
    {
        return Mono.just(
                new MappingDMO(
                        Constants.SYSTEM_ID,
                        versionedMappable.getId(),
                        mappingTypeId,
                        externalMethod.getInput(),
                        externalMethod.getOutput(),
                        "",
                        DistributionDMO.UNKNOWN
                )
        )
                .flatMap(mappingRepository::save)
                .doOnNext(this::setMapping)
                .then(Mono.just(this));
    }

    private Mono<MethodSaveHandler> createReleaseComponent(
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Mono.just(
                new ReleaseComponentDMO(release.getId(), mapping.getId())
        )
                .flatMap(releaseComponentRepository::save)
                .then(Mono.just(this));
    }

    private void setMappable(final MappableDMO mappable) {
        this.mappable = mappable;
    }

    private void setVersionedMappable(final VersionedMappableDMO versionedMappable) {
        this.versionedMappable = versionedMappable;
    }

    private void setMapping(final MappingDMO mapping) {
        this.mapping = mapping;
    }
}
