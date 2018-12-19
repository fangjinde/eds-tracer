package com.netease.edu.eds.trace.serve.service.impl;

import com.netease.edu.eds.shuffle.server.service.ShuffleService;
import com.netease.edu.eds.trace.serve.props.ShuffleServerProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
@Component("shuffleServiceImpl")
public class ShuffleServiceImpl implements ShuffleService {

    private static final Logger     logger                   = LoggerFactory.getLogger(ShuffleServiceImpl.class);
    private static final String     SHUFFLE_CACHE_KEY_PREFIX = "shuffle_kv_";
    @Autowired
    private StringRedisTemplate     stringRedisTemplate;
    @Autowired
    private ShuffleServerProperties shuffleServerProperties;

    @Override
    public String getValue(String key) {

        if (StringUtils.isBlank(key)) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(getKeyWithPrefix(key));

    }

    @Override
    public void setValue(String key, String value, Integer expireInSeconds) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return;
        }

        if (expireInSeconds <= 0) {
            expireInSeconds = shuffleServerProperties.getShuffleCacheExpireSeconds();
        }

        stringRedisTemplate.opsForValue().set(key, value, expireInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Long increment(String key, long delta, Integer expireInSeconds) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }

        if (expireInSeconds <= 0) {
            expireInSeconds = shuffleServerProperties.getShuffleCacheExpireSeconds();
        }

        Long curCount = stringRedisTemplate.opsForValue().increment(key, delta);
        stringRedisTemplate.expire(key, expireInSeconds, TimeUnit.SECONDS);
        return curCount;
    }

    private static String getKeyWithPrefix(String rawKey) {

        if (rawKey == null) {
            return null;
        }
        return SHUFFLE_CACHE_KEY_PREFIX + rawKey;

    }
}
