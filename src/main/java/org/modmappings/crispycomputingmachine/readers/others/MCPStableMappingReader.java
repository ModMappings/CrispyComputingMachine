package org.modmappings.crispycomputingmachine.readers.others;

import org.modmappings.crispycomputingmachine.model.mappings.ExternalMapping;
import org.modmappings.crispycomputingmachine.utils.Constants;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MCPStableMappingReader extends AbstractMavenBasedMappingReader
{
    public MCPStableMappingReader(
      final CompositeItemProcessor<String, List<ExternalMapping>> internalMCPStableMappingReaderProcessor
    )
    {
        super(internalMCPStableMappingReaderProcessor,
          Constants.MCP_MAPPING_NAME,
          Constants.MCP_STABLE_MAVEN_METADATA_FILE,
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
                  {
                      return rightParts.length - leftParts.length;
                  }

                  if (leftParts.length != 2)
                  {
                      return 0;
                  }

                  final String leftDatePart = leftParts[0];
                  final String rightDatePart = rightParts[0];
                  final LocalDate leftDate = LocalDate.parse(leftDatePart, DateTimeFormatter.BASIC_ISO_DATE);
                  final LocalDate rightDate = LocalDate.parse(rightDatePart, DateTimeFormatter.BASIC_ISO_DATE);

                  final int dateComparison = leftDate.compareTo(rightDate);
                  if (dateComparison != 0)
                  {
                      return dateComparison * -1;
                  }

                  return rightParts[1].compareTo(leftParts[1]);
              });

              return parsedVersions;
          });
    }
}
