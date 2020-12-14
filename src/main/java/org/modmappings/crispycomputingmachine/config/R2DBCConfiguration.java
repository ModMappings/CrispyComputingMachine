package org.modmappings.crispycomputingmachine.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.modmappings.mmms.er2dbc.data.access.strategy.ExtendedDataAccessStrategy;
import org.modmappings.mmms.er2dbc.relational.postgres.codec.EnumCodec;
import org.modmappings.mmms.repository.repositories.Repositories;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.TransactionManager;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Priority;
import javax.sql.DataSource;
import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackageClasses = Repositories.class)
class R2DBCConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "reactiveDataAccessStrategy")
    public ExtendedDataAccessStrategy reactiveDataAccessStrategy(final ExtendedDataAccessStrategy extendedDataAccessStrategy)
    {
        return extendedDataAccessStrategy;
    }

    @Bean()
    @Primary
    @Order(Integer.MIN_VALUE)
    public TransactionManager preferredTransactionManager(R2dbcTransactionManager transactionManager)
    {
        return transactionManager;
    }

    @Bean
    @Primary
    @Order(Integer.MIN_VALUE)
    public ConnectionFactoryOptionsBuilderCustomizer customEncoderCustomizer()
    {
        return builder -> builder.option(PostgresqlConnectionFactoryProvider.AUTODETECT_EXTENSIONS, true);
    }
}