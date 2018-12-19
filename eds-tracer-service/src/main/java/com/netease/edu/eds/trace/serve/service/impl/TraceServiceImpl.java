package com.netease.edu.eds.trace.serve.service.impl;

import com.netease.edu.eds.trace.serve.props.TraceServerProperties;
import com.netease.edu.eds.trace.server.service.TraceService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/12/12
 **/
@Component("traceServiceImpl")
public class TraceServiceImpl implements TraceService {

    private static final String   TRACE_CONTEXT_KEY_PREFIX = "trace_ctx_";
    private static final Logger   logger                   = LoggerFactory.getLogger(TraceServiceImpl.class);
    @Autowired
    private TraceServerProperties traceServerProperties;

    @Autowired
    private StringRedisTemplate   stringRedisTemplate;

    @Override
    public String getTraceContextByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }

        String key = getKeyWithPrefix(uuid);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Throwable e) {
            logger.error(String.format("unsafeGet redis value error, on key=%s", key), e);
            return null;
        }
    }

    @Override
    public void setTraceContextByUuid(String uuid, String traceContext) {
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(traceContext)) {
            return;
        }
        String key = getKeyWithPrefix(uuid);

        try {
            stringRedisTemplate.opsForValue().set(key, traceContext,
                                                  traceServerProperties.getTraceContextCacheExpireSeconds(),
                                                  TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error(String.format("unsafeSet redis key-value error, on key=%s, value=%s", key, traceContext), e);
        }

    }

    private static String getKeyWithPrefix(String rawKey) {

        if (rawKey == null) {
            return null;
        }
        return TRACE_CONTEXT_KEY_PREFIX + rawKey;

    }
}
