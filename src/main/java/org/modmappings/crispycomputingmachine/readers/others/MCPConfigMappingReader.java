package org.modmappings.crispycomputingmachine.readers.others;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MCPConfigMappingReader extends AbstractMavenBasedMappingReader {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.hhmmss");

    public MCPConfigMappingReader(final CompositeItemProcessor<String, List<ExternalMapping>> internalMCPConfigMappingReaderProcessor) {
        super(internalMCPConfigMappingReaderProcessor,
          Constants.MCP_CONFIG_MAPPING_NAME,
          Constants.MCP_CONFIG_MAVEN_METADATA_FILE,
          parsedVersions -> {
              parsedVersions.sort((l, r) -> {
                  if (l.equals(r))
                  {
                      return 0;
                  }

                  if (l.contains("-") && !r.contains("-"))
                  {
                      return -1;
                  }

                  if (r.contains("-") && !l.contains("-"))
                  {
                      return 1;
                  }

                  if (!r.contains("-") && !l.contains("-"))
                  {
                      return l.compareTo(r);
                  }

                  final String[] leftParts = l.split("-");
                  final String[] rightParts = r.split("-");
                  if (leftParts.length != rightParts.length)
                      return rightParts.length - leftParts.length;

                  if (leftParts.length >= 3 || leftParts.length == 0)
                      return 0;

                  final int versionComparison = leftParts[0].compareTo(rightParts[0]) * -1;
                  if (versionComparison != 0 || leftParts.length == 1)
                      return versionComparison;

                  final String leftDatePart = leftParts[1];
                  final String rightDatePart = rightParts[1];
                  final LocalDate leftDate = LocalDate.parse(leftDatePart, DATE_TIME_FORMATTER);
                  final LocalDate rightDate = LocalDate.parse(rightDatePart, DATE_TIME_FORMATTER);

                  return leftDate.compareTo(rightDate);
              });

              return parsedVersions;
          });
    }

}
