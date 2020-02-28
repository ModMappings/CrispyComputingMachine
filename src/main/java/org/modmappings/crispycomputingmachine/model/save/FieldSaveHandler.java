package org.modmappings.crispycomputingmachine.model.save;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalClass;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalField;
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

public class FieldSaveHandler {

    private final UUID mappingTypeId;
    private final ReleaseDMO release;
    private final ExternalField externalField;
    private final VersionedMappableDMO classVersionedMappable;

    private MappableDMO mappable;
    private VersionedMappableDMO versionedMappable;
    private MappingDMO mapping;
    private ReleaseComponentDMO releaseComponent;

    private FieldSaveHandler(final UUID mappingTypeId, final ReleaseDMO release, final ExternalField externalField, final VersionedMappableDMO classVersionedMappable) {
        this.mappingTypeId = mappingTypeId;
        this.release = release;
        this.externalField = externalField;
        this.classVersionedMappable = classVersionedMappable;
    }

    public static Mono<FieldSaveHandler> createAndRunSave(
            final UUID mappingTypeId,
            final ReleaseDMO release,
            final ExternalField externalClass,
            final VersionedMappableDMO classVersionedMappable,
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository,
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Mono.just(
                new FieldSaveHandler(mappingTypeId, release, externalClass, classVersionedMappable)
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

    private Mono<FieldSaveHandler> determineOrCreateAndSaveMappable(
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
                        MappableTypeDMO.FIELD,
                        previousClassVm.getId(),
                        null,
                        null,
                        null,
                        null,
                        Pageable.unpaged()
                    )
                    .flatMapIterable(Function.identity())
                )
                .flatMap(vmd -> mappingRepository.findAllOrLatestFor(
                        true,
                        vmd.getId(),
                        null,
                        MappableTypeDMO.FIELD,
                        null,
                        RegexUtils.createFullWordRegex(externalField.getOutput()),
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
                .flatMap(sameFieldInPreviousVersionMapping -> versionedMappableRepository.findById(sameFieldInPreviousVersionMapping.getVersionedMappableId()))
                .flatMap(sameFieldInPreviousVersionVM -> mappableRepository.findById(sameFieldInPreviousVersionVM.getMappableId()))
                .switchIfEmpty(Mono.just(
                        new MappableDMO(Constants.SYSTEM_ID, MappableTypeDMO.FIELD)
                        )
                )
                .flatMap(mappableRepository::save)
                .doOnNext(this::setMappable)
                .then(Mono.just(this));
    }

    private Mono<FieldSaveHandler> createAndSaveVersionedMappable(
            final VersionedMappableRepository versionedMappableRepository
    )
    {
        return Mono.just(
                VersionedMappableDMO.newField(
                        Constants.SYSTEM_ID,
                        release.getGameVersionId(),
                        mappable.getId(),
                        ConversionUtils.toVisibilityDMO(externalField.getVisibility()),
                        externalField.isStatic(),
                        externalField.getType(),
                        classVersionedMappable.getId()
                )
        )
                .flatMap(versionedMappableRepository::save)
                .doOnNext(this::setVersionedMappable)
                .then(Mono.just(this));
    }

    private Mono<FieldSaveHandler> createMapping(
            final MappingRepository mappingRepository
    )
    {
        return Mono.just(
                new MappingDMO(
                        Constants.SYSTEM_ID,
                        versionedMappable.getId(),
                        mappingTypeId,
                        externalField.getInput(),
                        externalField.getOutput(),
                        "",
                        DistributionDMO.UNKNOWN
                )
        )
                .flatMap(mappingRepository::save)
                .doOnNext(this::setMapping)
                .then(Mono.just(this));
    }

    private Mono<FieldSaveHandler> createReleaseComponent(
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
