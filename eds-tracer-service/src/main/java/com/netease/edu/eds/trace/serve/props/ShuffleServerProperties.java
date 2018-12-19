package com.netease.edu.eds.trace.serve.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
@ConfigurationProperties(prefix = "shuffle.server")
public class ShuffleServerProperties {

    public static final int DEFAULT_SHUFFLE_CACHE_EXPIRE_SECONDS = 3 * 3600;

    private int             shuffleCacheExpireSeconds            = DEFAULT_SHUFFLE_CACHE_EXPIRE_SECONDS;

    public int getShuffleCacheExpireSeconds() {
        return shuffleCacheExpireSeconds;
    }

    public void setShuffleCacheExpireSeconds(int shuffleCacheExpireSeconds) {
        this.shuffleCacheExpireSeconds = shuffleCacheExpireSeconds;
    }
}
