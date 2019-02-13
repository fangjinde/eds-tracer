package com.netease.edu.eds.trace.instrument.memcache;/**
 * Created by hzfjd on 18/4/19.
 */

import com.google.auto.value.AutoValue;

import brave.Tracing;

/**
 * @author hzfjd
 * @create 18/4/19
 */
@AutoValue
public abstract class MemcacheTracing {

    public static Builder newBuilder(Tracing tracing) {
        return newBuilder().tracing(tracing);
    }

    public static Builder newBuilder() {
        return new AutoValue_MemcacheTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract MemcacheTracing build();

        Builder() {
        }
    }

    MemcacheTracing() {
    }
}
