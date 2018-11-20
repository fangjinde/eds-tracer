package com.netease.edu.eds.trace.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 在spring容器实例化ConfigurationPropertiesBindingPostProcessor.class前的应用场景，是无法通过获取TraceProperties来获取值。典型场景就是在postProcessBeanFactory，
 * * * * 此时调用，或导致TraceProperties对象没有被ConfigurationPropertiesBindingPostProcessor处理。 这些地方改成从Environment中直接获取。 * * @See
 * TracePropertiesSupport.class *
 * 安全起见，生命周期不能确定是否晚于ConfigurationPropertiesBindingPostProcessor实例化的，统一直接从Environment中获取。具体参见TracePropertiesSupport
 * 
 * @author hzfjd
 * @create 18/11/7
 **/
@ConfigurationProperties(prefix = "trace")
public class TraceProperties {

    public static final String  DEFAULT_ENCODING                            = "UTF-8";
    public static final boolean DEFAULT_FORCE_ENCODING                      = true;
    public static final int     DEFAULT_REDIRECT_TRACE_CACHE_EXPIRE_SECONDS = 3 * 60;

    private Http                http                                        = new Http();
    private Cache               cache                                       = new Cache();

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public static class Http {

        private String  encoding                        = DEFAULT_ENCODING;
        private boolean forceEncoding                   = DEFAULT_FORCE_ENCODING;
        private int     redirectTraceCacheExpireSeconds = DEFAULT_REDIRECT_TRACE_CACHE_EXPIRE_SECONDS;

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public boolean isForceEncoding() {
            return forceEncoding;
        }

        public void setForceEncoding(boolean forceEncoding) {
            this.forceEncoding = forceEncoding;
        }

        public int getRedirectTraceCacheExpireSeconds() {
            return redirectTraceCacheExpireSeconds;
        }

        public void setRedirectTraceCacheExpireSeconds(int redirectTraceCacheExpireSeconds) {
            this.redirectTraceCacheExpireSeconds = redirectTraceCacheExpireSeconds;
        }
    }

    public static class Cache {

        private static final long DEFAULT_MAXIMUM_SIZE        = 10000;
        private static final long DEFAULT_EXPIRE_AFTER_ACCESS = 100;

        private long              maximumSize                 = DEFAULT_MAXIMUM_SIZE;
        private long              expireAfterAccess           = DEFAULT_EXPIRE_AFTER_ACCESS;

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public long getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public void setExpireAfterAccess(long expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }
    }
}
