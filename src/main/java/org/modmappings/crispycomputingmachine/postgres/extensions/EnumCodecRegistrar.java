package org.modmappings.crispycomputingmachine.postgres.extensions;

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.codec.CodecRegistry;
import io.r2dbc.postgresql.extension.CodecRegistrar;
import org.modmappings.mmms.er2dbc.relational.postgres.codec.EnumCodec;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class EnumCodecRegistrar implements CodecRegistrar {
    @Override
    public Publisher<Void> register(final PostgresqlConnection postgresqlConnection, final ByteBufAllocator byteBufAllocator, final CodecRegistry codecRegistry) {
        codecRegistry.addFirst(new EnumCodec(byteBufAllocator));
        return Mono.empty();
    }
}
