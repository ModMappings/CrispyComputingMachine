package org.modmappings.crispycomputingmachine.model.save;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalClass;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.crispycomputingmachine.utils.ConversionUtils;
import org.modmappings.crispycomputingmachine.utils.RegexUtils;
import org.modmappings.mmms.repository.model.core.release.ReleaseComponentDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.InheritanceDataDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.MappableTypeDMO;
import org.modmappings.mmms.repository.model.mapping.mappable.VersionedMappableDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.DistributionDMO;
import org.modmappings.mmms.repository.model.mapping.mappings.MappingDMO;
import org.modmappings.mmms.repository.repositories.core.releases.components.ReleaseComponentRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.inheritancedata.InheritanceDataRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.mappable.MappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.versionedmappables.VersionedMappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappings.mapping.MappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ClassSaveHandler {

    private static final Logger LOGGER = LogManager.getLogger(ClassSaveHandler.class);

    private final UUID mappingTypeId;
    private final ReleaseDMO release;
    private final ExternalClass externalClass;

    private MappableDMO mappable;
    private VersionedMappableDMO versionedMappable;
    private MappingDMO mapping;
    private ReleaseComponentDMO releaseComponent;

    private UUID outerClassVersionedMappableId = null;

    private ClassSaveHandler(final UUID mappingTypeId, final ReleaseDMO release, final ExternalClass externalClass) {
        this.mappingTypeId = mappingTypeId;
        this.release = release;
        this.externalClass = externalClass;
    }

    public static Mono<ClassSaveHandler> createAndRunInitialSave(
            final UUID mappingTypeId,
            final ReleaseDMO release,
            final ExternalClass externalClass,
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository,
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Mono.just(
                new ClassSaveHandler(mappingTypeId, release, externalClass)
            )
            .flatMap(csv -> csv.determineOrCreateAndSaveMappable(
                    mappingRepository,
                    versionedMappableRepository,
                    mappableRepository
                )
            )
            .flatMap(csv -> csv.determineOuterClass(
                    mappingRepository
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

    private Mono<ClassSaveHandler> determineOrCreateAndSaveMappable(
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository
            )
    {
        return mappingRepository.findAllOrLatestFor(
                false,
                null,
                null,
                MappableTypeDMO.CLASS,
                null,
                RegexUtils.createClassTargetingRegex(externalClass.getOutput()),
                mappingTypeId,
                null,
                null,
                false,
                PageRequest.of(0,1)
            )
            .flatMapIterable(Function.identity())
            .switchIfEmpty(Mono.<MappingDMO>empty().doFirst(() -> LOGGER.warn("Could not find existing mapping for: " + externalClass.getOutput())))
            .next()
            .flatMap(existingMapping -> versionedMappableRepository.findAllFor(
                    null,
                    null,
                    null,
                    null,
                    existingMapping.getId(),
                    null,
                    null,
                    PageRequest.of(0,1)
                    )
                    .flatMapIterable(Function.identity())
                    .next()
            )
                .switchIfEmpty(Mono.<VersionedMappableDMO>empty().doFirst(() -> LOGGER.warn("Could not find versioned mappable for: " + externalClass.getOutput())))
            .flatMap(previousVersionedMappable -> mappableRepository.findById(previousVersionedMappable.getMappableId()))
            .switchIfEmpty(Mono.just(
                    new MappableDMO(Constants.SYSTEM_ID, MappableTypeDMO.CLASS)
                )
                .flatMap(mappableRepository::save)
                .doFirst(() -> LOGGER.warn("Creating new Mappable for: " + externalClass.getOutput()))
            )
            .doOnNext(this::setMappable)
            .then(Mono.just(this));
    }

    private Mono<ClassSaveHandler> determineOuterClass(
            final MappingRepository mappingRepository
    )
    {
        if (!externalClass.getOutput().contains("$"))
            return Mono.just(this);

        return mappingRepository.findAllOrLatestFor(
                false,
                null,
                release.getId(),
                MappableTypeDMO.CLASS,
                null,
                RegexUtils.createOuterClassTargetingRegex(externalClass.getOutput()),
                mappingTypeId,
                release.getGameVersionId(),
                Constants.SYSTEM_ID,
                false,
                PageRequest.of(0,1)
            )
            .flatMapIterable(Function.identity())
            .next()
            .map(MappingDMO::getVersionedMappableId)
            .doOnNext(this::setOuterClassVersionedMappableId)
            .then(Mono.just(this));
    }

    private Mono<ClassSaveHandler> createAndSaveVersionedMappable(
            final VersionedMappableRepository versionedMappableRepository
    )
    {
        return Mono.just(
                new VersionedMappableDMO(
                    Constants.SYSTEM_ID,
                    release.getGameVersionId(),
                    mappable.getId(),
                    ConversionUtils.toVisibilityDMO(externalClass.getVisibility()),
                    externalClass.isStatic(),
                    null,
                    outerClassVersionedMappableId,
                    null,
                    null
                )
            )
            .flatMap(versionedMappableRepository::save)
            .doOnNext(this::setVersionedMappable)
            .then(Mono.just(this));
    }

    private Mono<ClassSaveHandler> createMapping(
            final MappingRepository mappingRepository
    )
    {
        return Mono.just(
                new MappingDMO(
                    Constants.SYSTEM_ID,
                    versionedMappable.getId(),
                    mappingTypeId,
                    externalClass.getInput(),
                    externalClass.getOutput(),
                    "",
                    DistributionDMO.UNKNOWN
                )
            )
            .flatMap(mappingRepository::save)
            .doOnNext(this::setMapping)
            .then(Mono.just(this));
    }

    private Mono<ClassSaveHandler> createReleaseComponent(
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Mono.just(
                new ReleaseComponentDMO(release.getId(), mapping.getId())
            )
            .flatMap(releaseComponentRepository::save)
            .then(Mono.just(this));
    }

    public Mono<ClassSaveHandler> createInheritanceData(
            final Map<ExternalClass, ClassSaveHandler> saveHandlers,
            final InheritanceDataRepository inheritanceDataRepository
    )
    {
        return Flux.fromIterable(externalClass.getSuperClasses())
                .map(superClass -> new InheritanceDataDMO(saveHandlers.get(superClass).getVersionedMappable().getId(), getVersionedMappable().getId()))
                .flatMap(inheritanceDataRepository::save)
                .collectList()
                .then(Mono.just(this));
    }

    public Mono<ClassSaveHandler> createAndSaveFieldData(
            final UUID mappingTypeId,
            final ReleaseDMO release,
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository,
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Flux.fromIterable(externalClass.getFields())
                .doOnNext(eField -> LOGGER.info("Dispatching saving of field: " + eField.getOutput() + " in " + externalClass.getOutput()))
                .flatMap(externalField -> FieldSaveHandler.createAndRunSave(mappingTypeId, release, externalField, versionedMappable, mappingRepository, versionedMappableRepository, mappableRepository, releaseComponentRepository), 1)
                .then(Mono.just(this));
    }

    public Mono<ClassSaveHandler> createAndSaveMethodData(
            final UUID mappingTypeId,
            final ReleaseDMO release,
            final MappingRepository mappingRepository,
            final VersionedMappableRepository versionedMappableRepository,
            final MappableRepository mappableRepository,
            final ReleaseComponentRepository releaseComponentRepository
    )
    {
        return Flux.fromIterable(externalClass.getMethods())
                .doOnNext(eMethod -> LOGGER.info("Dispatching saving of method: " + eMethod.getOutput() + " in " + externalClass.getOutput()))
                .flatMap(externalMethod -> MethodSaveHandler.createAndRunSave(mappingTypeId, release, externalMethod, versionedMappable, mappingRepository, versionedMappableRepository, mappableRepository, releaseComponentRepository), 1)
                .then(Mono.just(this));
    }

    public ExternalClass getExternalClass() {
        return externalClass;
    }

    public MappableDMO getMappable() {
        return mappable;
    }

    public VersionedMappableDMO getVersionedMappable() {
        return versionedMappable;
    }

    public MappingDMO getMapping() {
        return mapping;
    }

    public ReleaseComponentDMO getReleaseComponent() {
        return releaseComponent;
    }

    public UUID getOuterClassVersionedMappableId() {
        return outerClassVersionedMappableId;
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

    private void setOuterClassVersionedMappableId(final UUID outerClassVersionedMappableId) {
        this.outerClassVersionedMappableId = outerClassVersionedMappableId;
    }
}
