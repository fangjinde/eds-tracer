package com.netease.edu.eds.trace.serve.service.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.netease.edu.eds.trace.serve.service.TraceService;

/**
 * @author hzfjd
 * @create 18/12/12
 **/
public class TraceServiceImpl implements TraceService {

    private static final String TRACE_HTTP_REDIRECT_UNIQUE_KEY_PREFIX = "trace_hruk_";
    private static final Logger logger                                = LoggerFactory.getLogger(TraceServiceImpl.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String getTraceContextByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }

        String key=getTraceUniqueKeyWithCachePrefix(uuid);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Throwable e) {
            logger.error(String.format("unsafeGet redis value error, on key=%s", key), e);
            return null;
        }
    }

    @Override
    public void setTraceContextByUuid(String uuid, String traceContext) {
        if (StringUtils.isBlank(uuid)||StringUtils.isBlank(traceContext)) {
            return ;
        }
        String key=getTraceUniqueKeyWithCachePrefix(uuid);

    }

    private static String getTraceUniqueKeyWithCachePrefix(String rawKey) {

        if (rawKey == null) {
            return null;
        }
        return TRACE_HTTP_REDIRECT_UNIQUE_KEY_PREFIX + rawKey;

    }
}
