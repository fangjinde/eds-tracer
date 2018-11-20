package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.properties.TraceProperties;

/**
 * @author hzfjd
 * @create 18/11/7
 **/
public class TracePropertiesSupport {

    public static String getHttpRequestEncoding() {
        TraceProperties traceProperties = SpringBeanFactorySupport.getBean(TraceProperties.class);
        if (traceProperties == null) {
            return TraceProperties.DEFAULT_ENCODING;
        }
        return traceProperties.getHttp().getEncoding();
    }

    public static boolean isHttpRequestForceEncoding() {
        TraceProperties traceProperties = SpringBeanFactorySupport.getBean(TraceProperties.class);
        if (traceProperties == null) {
            return TraceProperties.DEFAULT_FORCE_ENCODING;
        }
        return traceProperties.getHttp().isForceEncoding();
    }

    public static int getRedirectTraceCacheExpireSeconds() {
        TraceProperties traceProperties = SpringBeanFactorySupport.getBean(TraceProperties.class);
        if (traceProperties == null) {
            return TraceProperties.DEFAULT_REDIRECT_TRACE_CACHE_EXPIRE_SECONDS;
        }
        return traceProperties.getHttp().getRedirectTraceCacheExpireSeconds();
    }

}
