package com.netease.edu.eds.trace.instrument.rabbit;/**
                                                     * Created by hzfjd on 18/4/12.
                                                     */

import brave.Tracing;
import com.google.auto.value.AutoValue;

/**
 * @author hzfjd
 * @create 18/4/12
 */
@AutoValue
public abstract class RabbitTracing {

    static final String RABBIT_EXCHANGE = "rabbit.exchange", RABBIT_ROUTING_KEY = "rabbit.routing_key",
            RABBIT_QUEUE = "rabbit.queue", RABBIT_PAYLOAD = "rabbit.payload";

    public abstract String remoteServiceName();

    public abstract Tracing tracing();

    public static Builder newBuilder() {
        return new AutoValue_RabbitTracing.Builder().remoteServiceName("rabbit");
    }

    public abstract Builder toBuilder();

    RabbitTracing() {

    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder remoteServiceName(String remoteServiceName);

        public abstract Builder tracing(Tracing tracing);

        public abstract RabbitTracing build();

        Builder() {

        }
    }

}
