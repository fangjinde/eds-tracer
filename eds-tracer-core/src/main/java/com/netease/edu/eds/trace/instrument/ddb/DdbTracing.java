package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/22.
 */

import brave.Tracing;
import com.google.auto.value.AutoValue;

/**
 * @author hzfjd
 * @create 18/3/22
 */
@AutoValue
public abstract class DdbTracing {

    public static Builder newBuilder(Tracing tracing) {
        return newBuilder().tracing(tracing);
    }

    public static Builder newBuilder() {
        return new AutoValue_DdbTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract DdbTracing build();

        Builder() {
        }
    }

    DdbTracing() {
    }
}
