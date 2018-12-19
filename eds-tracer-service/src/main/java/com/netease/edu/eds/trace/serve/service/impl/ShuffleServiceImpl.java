package com.netease.edu.eds.trace.serve.service.impl;

import com.netease.edu.eds.shuffle.server.service.ShuffleService;
import com.netease.edu.eds.trace.serve.props.ShuffleServerProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
@Component("shuffleServiceImpl")
@RestController
public class ShuffleServiceImpl implements ShuffleService {

    private static final Logger     logger                   = LoggerFactory.getLogger(ShuffleServiceImpl.class);
    private static final String     SHUFFLE_CACHE_KEY_PREFIX = "shuffle_kv_";
    @Autowired
    private StringRedisTemplate     stringRedisTemplate;
    @Autowired
    private ShuffleServerProperties shuffleServerProperties;

    @Override
    @RequestMapping(path = "/shuffle/value/{key}", method = RequestMethod.GET)
    public String getValue(@PathVariable("key") String key) {

        if (StringUtils.isBlank(key)) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(getKeyWithPrefix(key));

    }

    @Override
    @RequestMapping(path = "/shuffle/value/{key}", method = RequestMethod.POST)
    public void setValue(@PathVariable("key") String key, @RequestParam("value") String value,
                         @RequestParam(value = "expireInSeconds", required = false) Integer expireInSeconds) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return;
        }

        if (expireInSeconds <= 0) {
            expireInSeconds = shuffleServerProperties.getShuffleCacheExpireSeconds();
        }

        stringRedisTemplate.opsForValue().set(key, value, expireInSeconds, TimeUnit.SECONDS);
    }

    @Override
    @RequestMapping(path = "/shuffle/value/{key}/increase", method = RequestMethod.POST)
    public Long increment(@PathVariable("key") String key, @RequestParam(value = "delta", required = false) Long delta,
                          @RequestParam(value = "expireInSeconds", required = false) Integer expireInSeconds) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }

        if (delta == null) {
            delta = 1L;
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
