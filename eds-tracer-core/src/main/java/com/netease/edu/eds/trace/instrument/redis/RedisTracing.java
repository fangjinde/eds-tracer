package com.netease.edu.eds.trace.instrument.redis;/**
 * Created by hzfjd on 18/4/19.
 */

import brave.Tracing;
import com.google.auto.value.AutoValue;

/**
 * @author hzfjd
 * @create 18/4/19
 */
@AutoValue
public abstract class RedisTracing {

    public static Builder newBuilder(Tracing tracing) {
        return newBuilder().tracing(tracing);
    }

    public static Builder newBuilder() {
        return new AutoValue_RedisTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract RedisTracing build();

        Builder() {
        }
    }

    RedisTracing() {
    }
}
