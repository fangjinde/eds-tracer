package com.netease.edu.eds.trace.instrument.elasticsearch;

import brave.Tracing;

import com.google.auto.value.AutoValue;
import com.netease.edu.eds.trace.instrument.ndir.NdirTracing.Builder;

@AutoValue
public abstract class ElasticsearchTracing {

    public static Builder newBuilder() {
        return new AutoValue_ElasticsearchTracing.Builder();
    }

    public abstract Tracing tracing();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder tracing(Tracing tracing);

        public abstract ElasticsearchTracing build();

        Builder() {
        }
    }

    ElasticsearchTracing() {
    }
}
