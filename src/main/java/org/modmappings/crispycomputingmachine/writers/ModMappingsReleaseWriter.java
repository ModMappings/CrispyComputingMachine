package org.modmappings.crispycomputingmachine.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalClass;
import org.modmappings.crispycomputingmachine.model.mappings.ExternalRelease;
import org.modmappings.crispycomputingmachine.model.save.ClassSaveHandler;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.modmappings.mmms.repository.model.core.GameVersionDMO;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.repositories.core.gameversions.GameVersionRepository;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.modmappings.mmms.repository.repositories.core.releases.components.ReleaseComponentRepository;
import org.modmappings.mmms.repository.repositories.core.releases.release.ReleaseRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.inheritancedata.InheritanceDataRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.mappable.MappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappables.versionedmappables.VersionedMappableRepository;
import org.modmappings.mmms.repository.repositories.mapping.mappings.mapping.MappingRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModMappingsReleaseWriter implements ItemWriter<ExternalRelease> {

    private static final Logger LOGGER = LogManager.getLogger(ModMappingsReleaseWriter.class);

    private final ReleaseRepository releaseRepository;

    private final MappableRepository mappableRepository;
    private final VersionedMappableRepository versionedMappableRepository;
    private final MappingRepository mappingRepository;
    private final ReleaseComponentRepository releaseComponentRepository;
    private final InheritanceDataRepository inheritanceDataRepository;
    private final GameVersionRepository gameVersionRepository;
    private final MappingTypeRepository mappingTypeRepository;

    public ModMappingsReleaseWriter(final ReleaseRepository releaseRepository, final MappableRepository mappableRepository, final VersionedMappableRepository versionedMappableRepository, final MappingRepository mappingRepository, final ReleaseComponentRepository releaseComponentRepository, final InheritanceDataRepository inheritanceDataRepository, final GameVersionRepository gameVersionRepository, final MappingTypeRepository mappingTypeRepository) {
        this.releaseRepository = releaseRepository;
        this.mappableRepository = mappableRepository;
        this.versionedMappableRepository = versionedMappableRepository;
        this.mappingRepository = mappingRepository;
        this.releaseComponentRepository = releaseComponentRepository;
        this.inheritanceDataRepository = inheritanceDataRepository;
        this.gameVersionRepository = gameVersionRepository;
        this.mappingTypeRepository = mappingTypeRepository;
    }

    @Override
    public void write(final List<? extends ExternalRelease> items) throws Exception {
        getOfficialMappingType()
                .flatMapMany(offMapType -> Flux.fromIterable(items)
                        .flatMap(eRel -> createNewGameVersion(eRel)
                                .map(gv -> new ReleaseDMO(
                                                Constants.SYSTEM_ID,
                                                eRel.getName(),
                                                gv.getId(),
                                                offMapType.getId(),
                                                eRel.isSnapshot() || eRel.isPreRelease()
                                        )
                                )
                                .flatMap(releaseRepository::save)
                                .flatMap(release -> {
                                            final Map<Long, List<ExternalClass>> classesToProcess = eRel.getClasses().stream().sorted(Comparator.comparing(ExternalClass::getOutput)).collect(Collectors.groupingBy(ec -> ec.getOutput().chars().filter(ch -> ch == '$').count()));

                                            return Flux.fromIterable(classesToProcess.entrySet())
                                                    .sort(Map.Entry.comparingByKey())
                                                    .doOnNext(g -> LOGGER.info("Created class group with depth:" + g.getKey()))
                                                    .flatMapIterable(Map.Entry::getValue)
                                                    .collectList()
                                                    .flatMapIterable(Function.identity())
                                                    .doOnNext(eClass -> LOGGER.info("Starting initial save of: " + eClass.getOutput()))
                                                    .flatMap(eClass ->
                                                                    ClassSaveHandler.createAndRunInitialSave(
                                                                            offMapType.getId(),
                                                                            release,
                                                                            eClass,
                                                                            mappingRepository,
                                                                            versionedMappableRepository,
                                                                            mappableRepository,
                                                                            releaseComponentRepository
                                                                    )
                                                    )
                                                    .collectMap(
                                                            ClassSaveHandler::getExternalClass,
                                                            Function.identity()
                                                    )
                                                    .flatMap(saveHandlers -> Flux.fromIterable(saveHandlers.values())
                                                            .flatMap(csv -> csv.createInheritanceData(
                                                                    saveHandlers,
                                                                    inheritanceDataRepository
                                                                    )
                                                                            .doFirst(() -> LOGGER.info("Saving inheritance data for: " + csv.getExternalClass().getOutput()))
                                                            )
                                                            .collectList()
                                                            .flatMapIterable(Function.identity())
                                                            .flatMap(csv -> csv.createAndSaveFieldData(offMapType.getId(),
                                                                    release,
                                                                    mappingRepository, versionedMappableRepository, mappableRepository, releaseComponentRepository)
                                                                            .doFirst(() -> LOGGER.info("Saving field data for: " + csv.getExternalClass().getOutput()))
                                                            )
                                                            .collectList()
                                                            .flatMapIterable(Function.identity())
                                                            .flatMap(csv -> csv.createAndSaveMethodData(offMapType.getId(),
                                                                    release,
                                                                    mappingRepository, versionedMappableRepository, mappableRepository, releaseComponentRepository)
                                                                            .doFirst(() -> LOGGER.info("Saving method data for: " + csv.getExternalClass().getOutput()))
                                                            )
                                                            .then(Mono.just(saveHandlers))
                                                    );
                                        }

                                )
                        )
                )
                .then()
                .block();
    }

    private Mono<MappingTypeDMO> getOfficialMappingType() {
        return mappingTypeRepository.findAllBy("\\A" + Constants.OFFICIAL_MAPPING_NAME + "\\Z", null, false, PageRequest.of(0, 1))
                .flatMapIterable(Function.identity())
                .next()
                .switchIfEmpty(Mono.just(new MappingTypeDMO(Constants.SYSTEM_ID, Constants.OFFICIAL_MAPPING_NAME, false, false, Constants.OFFICIAL_MAPPING_STATE_IN, Constants.OFFICIAL_MAPPING_STATE_OUT))
                        .flatMap(mappingTypeRepository::save)
                );
    }

    private Mono<GameVersionDMO> createNewGameVersion(final ExternalRelease release) {
        return Mono.just(new GameVersionDMO(Constants.SYSTEM_ID, release.getName(), release.isPreRelease(), release.isSnapshot()))
                .flatMap(gameVersionRepository::save);
    }
}
