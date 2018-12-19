package com.netease.edu.eds.trace.serve.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
@ConfigurationProperties(prefix = "trace.server")
public class TraceServerProperties {

    public static final int DEFAULT_TRACE_CONTEXT_CACHE_EXPIRE_SECONDS = 3 * 60;
    private int             traceContextCacheExpireSeconds             = DEFAULT_TRACE_CONTEXT_CACHE_EXPIRE_SECONDS;

    public int getTraceContextCacheExpireSeconds() {
        return traceContextCacheExpireSeconds;
    }

    public void setTraceContextCacheExpireSeconds(int traceContextCacheExpireSeconds) {
        this.traceContextCacheExpireSeconds = traceContextCacheExpireSeconds;
    }

}
