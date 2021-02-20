package org.modmappings.crispycomputingmachine.processors.release;

import com.google.common.collect.Sets;
import net.minecraftforge.srgutils.IMappingFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modmappings.crispycomputingmachine.model.mappings.*;
import org.modmappings.crispycomputingmachine.utils.MethodDesc;
import org.modmappings.crispycomputingmachine.utils.ParameterRef;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class ExternalReleaseToExternalVanillaMappingProcessor implements ItemProcessor<ExternalRelease, List<ExternalVanillaMapping>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public List<ExternalVanillaMapping> process(final ExternalRelease item) throws Exception
    {
        return item.getClasses().stream().flatMap(externalClass -> {
            final List<ExternalVanillaMapping> mappingsForVersion = new ArrayList<>();

            String parentClassOutputMapping = null;
            if (externalClass.getOutput().contains("$"))
            {
                parentClassOutputMapping = externalClass.getOutput().substring(0, externalClass.getOutput().lastIndexOf("$"));
            }

            final ExternalVanillaMapping classMapping = new ExternalVanillaMapping(
              externalClass.getInput(),
              externalClass.getOutput(),
              ExternalMappableType.CLASS,
              item.getName(),
              item.getReleasedOn(),
              item.getName(),
              parentClassOutputMapping,
              null,
              null,
              externalClass.getVisibility(),
              externalClass.isStatic(),
              null,
              null,
              null,
              -1,
              externalClass.isExternal(),
              externalClass.getSuperClasses().stream().map(ExternalClass::getOutput).collect(Collectors.toList()),
              new HashSet<>(), Sets.newHashSet());

            final IMappingFile.IClass classClientMapping = item.getClientFile().findClassFromName(externalClass.getInput());
            final IMappingFile.IClass classServerMapping = item.getServerFile().findClassFromName(externalClass.getInput());

            final boolean isClassClientMapping = !classClientMapping.getOriginal().equals(classClientMapping.getMapped());
            final boolean isClassServerMapping = !classServerMapping.getOriginal().equals(classServerMapping.getMapped());

            classMapping.setExternalDistribution(
              isClassClientMapping
                ? (isClassServerMapping ? ExternalDistribution.COMMON : ExternalDistribution.CLIENT)
                : (isClassServerMapping ? ExternalDistribution.SERVER : ExternalDistribution.UNKNOWN)
            );

            mappingsForVersion.add(
              classMapping
            );

            externalClass.getMethods().stream().map(externalMethod -> {
                final ExternalVanillaMapping emm = new ExternalVanillaMapping(
                  externalMethod.getInput(),
                  externalMethod.getOutput(),
                  ExternalMappableType.METHOD,
                  item.getName(),
                  item.getReleasedOn(),
                  item.getName(),
                  externalClass.getOutput(),
                  null,
                  null,
                  externalMethod.getVisibility(),
                  externalMethod.isStatic(),
                  null,
                  externalMethod.getDescriptor(),
                  externalMethod.getSignature(),
                  -1,
                  externalMethod.isExternal(),
                  new ArrayList<>(),
                  externalMethod.getOverrides(),
                  Sets.newHashSet());

                final boolean isClientMapping = !classClientMapping.remapMethod(emm.getInput(), emm.getDescriptor()).equals(emm.getInput());
                final boolean isServerMapping = !classServerMapping.remapMethod(emm.getInput(), emm.getDescriptor()).equals(emm.getInput());

                emm.setExternalDistribution(
                  isClientMapping
                    ? (isServerMapping ? ExternalDistribution.COMMON : ExternalDistribution.CLIENT)
                    : (isServerMapping ? ExternalDistribution.SERVER : ExternalDistribution.UNKNOWN)
                );

                return emm;
            })
              .forEach(mappingsForVersion::add);

            externalClass.getMethods().stream().flatMap(externalMethod -> {
                  final MethodDesc desc = new MethodDesc(externalMethod.getDescriptor());
                  final AtomicInteger initialIndex = new AtomicInteger(externalMethod.isStatic() ? 0 : 1);

                  final boolean isClientMapping = !classClientMapping.remapMethod(externalMethod.getInput(), externalMethod.getDescriptor()).equals(externalMethod.getInput());
                  final boolean isServerMapping = !classServerMapping.remapMethod(externalMethod.getInput(), externalMethod.getDescriptor()).equals(externalMethod.getInput());

                  return desc.getArgs().stream().map(argType -> {
                      final ExternalVanillaMapping mapping = new ExternalVanillaMapping(
                        externalMethod.getInput() + "_" + initialIndex.get(),
                        externalMethod.getOutput() + "_" + initialIndex.get(),
                        ExternalMappableType.PARAMETER,
                        item.getName(),
                        item.getReleasedOn(),
                        item.getName(),
                        externalClass.getOutput(),
                        externalMethod.getOutput(),
                        externalMethod.getDescriptor(),
                        ExternalVisibility.NOT_APPLICABLE,
                        false,
                        argType,
                        null,
                        null,
                        initialIndex.get(),
                        false,
                        new ArrayList<>(),
                        Sets.newHashSet(),
                        externalMethod.getOverrides().stream().map(mr -> new ParameterRef(mr, initialIndex.get(), argType)).collect(Collectors.toSet()));

                      mapping.setExternalDistribution(
                        isClientMapping
                          ? (isServerMapping ? ExternalDistribution.COMMON : ExternalDistribution.CLIENT)
                          : (isServerMapping ? ExternalDistribution.SERVER : ExternalDistribution.UNKNOWN)
                      );

                      if (argType.equals("D") || argType.equals("J"))
                      {
                          initialIndex.incrementAndGet();
                      }
                      initialIndex.incrementAndGet();

                      return mapping;
                  });
              }
            ).forEach(mappingsForVersion::add);

            externalClass.getFields().stream().map(externalField -> {
                final ExternalVanillaMapping efm = new ExternalVanillaMapping(
                  externalField.getInput(),
                  externalField.getOutput(),
                  ExternalMappableType.FIELD,
                  item.getName(),
                  item.getReleasedOn(),
                  item.getName(),
                  externalClass.getOutput(),
                  null,
                  null,
                  externalField.getVisibility(),
                  externalField.isStatic(),
                  externalField.getType(),
                  null,
                  null,
                  -1,
                  false,
                  new ArrayList<>(),
                  new HashSet<>(), Sets.newHashSet());

                final boolean isClientMapping = !classClientMapping.remapField(efm.getInput()).equals(efm.getInput());
                final boolean isServerMapping = !classServerMapping.remapField(efm.getInput()).equals(efm.getInput());

                efm.setExternalDistribution(
                  isClientMapping
                    ? (isServerMapping ? ExternalDistribution.COMMON : ExternalDistribution.CLIENT)
                    : (isServerMapping ? ExternalDistribution.SERVER : ExternalDistribution.UNKNOWN)
                );

                return efm;
            })
              .forEach(mappingsForVersion::add);

            return mappingsForVersion.stream();
        }).collect(Collectors.toList());
    }
}
