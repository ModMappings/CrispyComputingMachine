package org.modmappings.crispycomputingmachine.config;

import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import org.modmappings.mmms.er2dbc.data.access.strategy.ExtendedDataAccessStrategy;
import org.modmappings.mmms.repository.repositories.Repositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.TransactionManager;

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