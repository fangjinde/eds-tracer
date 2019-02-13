package com.netease.edu.eds.trace.instrument.async;/**
 * Created by hzfjd on 18/3/22.
 */

import com.google.auto.value.AutoValue;

import brave.Tracing;

/**
 * @author hzfjd
 * @create 18/3/22
 */
@AutoValue
public abstract class AsyncTracing {

    public static Builder newBuilder(Tracing tracing) {
        return newBuilder().tracing(tracing);
    }

    public static Builder newBuilder() {
        return new AutoValue_AsyncTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract AsyncTracing build();

        Builder() {
        }
    }

    AsyncTracing() {
    }
}
