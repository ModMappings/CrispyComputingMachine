package org.modmappings.crispycomputingmachine.utils;

import org.springframework.data.r2dbc.core.DatabaseClient;

public class DatabaseUtils
{

    private DatabaseUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: DatabaseUtils. This is a utility class");
    }

    public static void updatePackages(final DatabaseClient databaseClient)
    {
        final int maxPackageDepth = databaseClient.execute("Select LENGTH(REPLACE(mapping.output,'/','~'))-LENGTH(REPLACE(mapping.output,'/','')) from mapping\n"
                                                             + "order by LENGTH(REPLACE(mapping.output,'/','~'))-LENGTH(REPLACE(mapping.output,'/','')) DESC\n"
                                                             + "limit 1").as(Integer.class).fetch().first().block() + 3; // Just to be sure.

        databaseClient.execute("alter table packages\n"
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
                                 + "for r in 1.." + maxPackageDepth +" loop\n"
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
}
