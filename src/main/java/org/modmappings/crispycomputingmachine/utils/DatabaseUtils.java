package org.modmappings.crispycomputingmachine.utils;

import io.r2dbc.spi.Statement;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.Parameter;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.data.relational.core.sql.IdentifierProcessing.NONE;

public class DatabaseUtils
{

    public static final String INSERT_INTO_ALL_SPEC = "INSERT INTO %s (%s) VALUES %s;";
    public static final String INSERT_INTO_SPEC = "INSERT INTO %s (%s) VALUES (%s);";

    private DatabaseUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: DatabaseUtils. This is a utility class");
    }

    public static void updatePackages(final R2dbcEntityTemplate databaseClient)
    {
        final int maxPackageDepth = databaseClient.getDatabaseClient().sql("Select LENGTH(REPLACE(mapping.output,'/','~'))-LENGTH(REPLACE(mapping.output,'/','')) from mapping\n"
                                                                             + "order by LENGTH(REPLACE(mapping.output,'/','~'))-LENGTH(REPLACE(mapping.output,'/','')) DESC\n"
                                                                             + "limit 1").map(new EntityRowMapper<>(Integer.class, databaseClient.getConverter())).first().block()
                                      + 3; // Just to be sure.

        databaseClient.getDatabaseClient().sql("alter table packages\n"
                                                 + "    drop constraint \"FK_packages_packages_parent\";"
                                                 + "\n"
                                                 + "insert into packages (path, name, parent_path, parent_parent_path)\n"
                                                 + "    select distinct on (coalesce(substring(m.output, '([a-zA-Z0-9$-_]+)\\/'), ''),  coalesce(substring(m.output, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)\\/[a-zA-Z0-9$\\-_]+'), ''))\n"
                                                 + "                    coalesce(substring(m.output, '([a-zA-Z0-9$-_]+)\\/'), '') as path,\n"
                                                 + "                    coalesce(substring(m.output, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)\\/[a-zA-Z0-9$\\-_]+'), '') as name,\n"
                                                 + "                    coalesce(substring(coalesce(substring(m.output, '([a-zA-Z0-9$-_]+)\\/'), ''), '([a-zA-Z0-9$-_]+)\\/'), '') as parent_path,\n"
                                                 + "                    coalesce(substring(coalesce(substring(coalesce(substring(m.output, '([a-zA-Z0-9$-_]+)\\/'), ''), '([a-zA-Z0-9$-_]+)\\/'), ''), '([a-zA-Z0-9$-_]+)\\/'), '') as parent_parent_path\n"
                                                 + "                    from mapping m\n"
                                                 + "                    where m.mappable_type = 'CLASS'\n"
                                                 + "                    order by coalesce(substring(m.output, '([a-zA-Z0-9$-_]+)\\/'), ''),  coalesce(substring(m.output, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)\\/[a-zA-Z0-9$\\-_]+'), '')\n"
                                                 + "on conflict do nothing ;\n"
                                                 + "\n"
                                                 + "\n"
                                                 + "do $$\n"
                                                 + "begin\n"
                                                 + "for r in 1.." + maxPackageDepth + " loop\n"
                                                 + "    insert into packages (path, name, parent_path, parent_parent_path)\n"
                                                 + "        select distinct on (p.parent_path, coalesce(substring(p.parent_path, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)'), ''))\n"
                                                 + "            p.parent_path                                                                                                                                         as path,\n"
                                                 + "            coalesce(substring(p.parent_path, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)'), '')                                                                   as name,\n"
                                                 + "            coalesce(substring(p.parent_path, '([a-zA-Z0-9$-_]+)\\/'), '')                                         as parent_path,\n"
                                                 + "            coalesce(substring(coalesce(substring(p.parent_path, '([a-zA-Z0-9$-_]+)\\/'), ''), '([a-zA-Z0-9$-_]+)\\/'), '')                   as parent_parent_path\n"
                                                 + "        from packages p\n"
                                                 + "        order by p.parent_path, coalesce(substring(p.parent_path, '(?:[a-zA-Z0-9$\\-_]+\\/)*([a-zA-Z0-9$\\-_]+)'), '')\n"
                                                 + "        on conflict do nothing;\n"
                                                 + "end loop;\n"
                                                 + "end$$;\n"
                                                 + "\n"
                                                 + "update mmms.public.mapping\n"
                                                 + "    set package_path = coalesce(substring(mapping.output, '([a-zA-Z0-9$-_]+)\\/'), ''),\n"
                                                 + "        package_parent_path = coalesce(substring(coalesce(substring(mapping.output, '([a-zA-Z0-9$-_]+)\\/'), ''), '([a-zA-Z0-9$-_]+)\\/'), '')\n"
                                                 + "where mappable_type = 'CLASS' and package_path is null and package_parent_path is null;\n"
                                                 + "\n"
                                                 + "alter table packages\n"
                                                 + "    add constraint \"FK_packages_packages_parent\" foreign key (parent_path, parent_parent_path) references packages (path, parent_path);\n"
                                                 + "").then().block();
    }

    public static <T> String createInsertForAll(final R2dbcEntityTemplate entityTemplate, Class<T> entityType, Iterable<T> entities) {
        if (!entities.iterator().hasNext())
            return "";

        final SqlIdentifier tableName = entityTemplate.getDataAccessStrategy().getTableName(entityType);
        final OutboundRow outboundRow = entityTemplate.getDataAccessStrategy().getOutboundRow(entities.iterator().next());

        final String keys = outboundRow.entrySet().stream()
                              .map(Map.Entry::getKey)
                              .map(s -> s.toSql(NONE))
                              .collect(Collectors.joining(", "));

        final String entityData = StreamSupport.stream(entities.spliterator(), false)
          .map(entity -> entityTemplate.getDataAccessStrategy().getOutboundRow(entity))
          .map(row -> row.entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(DatabaseUtils::convertParameter)
                        .collect(Collectors.joining(", ")))
          .map(rowData -> String.format("(%s)", rowData))
          .collect(Collectors.joining(", "));

        return String.format(INSERT_INTO_ALL_SPEC, tableName, keys, entityData);
    }

    public static <T> String createInsertFor(final R2dbcEntityTemplate entityTemplate, T entity)
    {
        final SqlIdentifier tableName = entityTemplate.getDataAccessStrategy().getTableName(entity.getClass());
        final OutboundRow outboundRow = entityTemplate.getDataAccessStrategy().getOutboundRow(entity);

        final String keys = outboundRow.entrySet().stream()
                              .filter(e -> e.getValue().hasValue())
                              .map(Map.Entry::getKey)
                              .map(s -> s.toSql(NONE))
                              .collect(Collectors.joining(", "));
        final String values = outboundRow.entrySet().stream()
                                .filter(e -> e.getValue().hasValue())
                                .map(Map.Entry::getValue)
                                .map(DatabaseUtils::convertParameter)
                                .collect(Collectors.joining(", "));

        return String.format(INSERT_INTO_SPEC, tableName, keys, values);
    }

    public static String convertParameter(final Parameter parameter)
    {
        if (!parameter.hasValue() || parameter.getValue() == null)
        {
            return "DEFAULT";
        }

        if (parameter.getValue() instanceof Number ||
              parameter.getValue() instanceof Boolean)
        {
            return parameter.getValue().toString();
        }

        return String.format("'%s'", parameter.getValue().toString());
    }

    public static Mono<Statement> createPrebuildSimpleStatement(final R2dbcEntityTemplate entityTemplate, final String sql)
    {
        return Mono.from(entityTemplate.getDatabaseClient().getConnectionFactory()
          .create())
          .map(connection -> connection.createStatement("SELECT 1"))
          .map(simpleStatement -> {
              try
              {
                  final Field sqlField = simpleStatement.getClass().getDeclaredField("sql");
                  sqlField.setAccessible(true);
                  sqlField.set(simpleStatement, sql);
                  return simpleStatement;
              }
              catch (NoSuchFieldException | IllegalAccessException e)
              {
                  throw new IllegalStateException("Failed to update statement sql.");
              }
          });
    }
}
