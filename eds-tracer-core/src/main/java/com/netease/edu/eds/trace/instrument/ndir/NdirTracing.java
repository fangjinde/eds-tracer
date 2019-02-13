package com.netease.edu.eds.trace.instrument.ndir;/**
 * Created by hzfjd on 18/4/19.
 */

import brave.Tracing;
import com.google.auto.value.AutoValue;

/**
 * @author hzfjd
 * @create 18/4/19
 */
@AutoValue
public abstract class NdirTracing {

    public static Builder newBuilder(Tracing tracing) {
        return newBuilder().tracing(tracing);
    }

    public static Builder newBuilder() {
        return new AutoValue_NdirTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract NdirTracing build();

        Builder() {
        }
    }

    NdirTracing() {
    }
}
