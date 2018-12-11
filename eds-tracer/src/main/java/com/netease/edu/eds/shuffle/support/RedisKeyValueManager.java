package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisOperations;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public class RedisKeyValueManager implements KeyValueManager {

    @Resource(name = BeanNameConstants.SHUFFLE_REDIS_TEMPLATE)
    RedisOperations<String, String> shuffleRedisTemplate;

    @Override
    public String getValue(String key) {

        if (StringUtils.isBlank(key)) {
            return null;
        }
        return shuffleRedisTemplate.opsForValue().get(key);

    }

    @Override
    public void setValue(String key, String value, Integer expireInSeconds) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        shuffleRedisTemplate.opsForValue().set(key, value);
        shuffleRedisTemplate.expire(key, expireInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Long increment(String key, long delta, Integer expireInSeconds) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }
        Long curCount = shuffleRedisTemplate.opsForValue().increment(key, delta);
        shuffleRedisTemplate.expire(key, expireInSeconds, TimeUnit.SECONDS);
        return curCount;
    }
}
