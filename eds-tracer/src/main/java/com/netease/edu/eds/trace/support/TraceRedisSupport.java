package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.constants.TraceBeanNameConstants;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisOperations;

import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/11/15
 **/
public class TraceRedisSupport {

    private static final Logger logger = LoggerFactory.getLogger(TraceRedisSupport.class);

    public static void unsafeSet(String key, Object value, long timeout, TimeUnit unit) {

        if (StringUtils.isBlank(key) || value == null) {
            return;
        }

        if (timeout <= 0) {
            timeout = 3*60;
        }

        if (unit == null) {
            unit = TimeUnit.SECONDS;
        }

        RedisOperations<String, Object> redisOperations = SpringBeanFactorySupport.getBean(TraceBeanNameConstants.TRACE_REDIS_TEMPLATE_BEAN_NAME);
        if (redisOperations == null) {
            return;
        }

        try {
            redisOperations.opsForValue().set(key, value, timeout, unit);
        } catch (Throwable e) {
            logger.error(String.format("unsafeSet redis key-value error, on key=%s, value=%s", key,
                                       TraceJsonUtils.toJson(value)),
                         e);
        }

    }

    public static Object unsafeGet(String key) {

        if (StringUtils.isBlank(key)) {
            return null;
        }

        RedisOperations<String, Object> redisOperations = SpringBeanFactorySupport.getBean(TraceBeanNameConstants.TRACE_REDIS_TEMPLATE_BEAN_NAME);
        if (redisOperations == null) {
            return null;
        }

        try {
            return redisOperations.opsForValue().get(key);
        } catch (Throwable e) {
            logger.error(String.format("unsafeGet redis value error, on key=%s", key), e);
            return null;
        }

    }

}
