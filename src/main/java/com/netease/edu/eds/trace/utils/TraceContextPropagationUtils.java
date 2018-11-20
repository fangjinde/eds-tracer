package com.netease.edu.eds.trace.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author hzfjd
 * @create 18/9/10
 **/
public class TraceContextPropagationUtils {

    private static final Logger logger                                = LoggerFactory.getLogger(TraceContextPropagationUtils.class);

    public static final String  TRACE_HTTP_REDIRECT_UNIQUE_KEY_PREFIX = "trace_hruk_";




    public static String generateTraceUniqueKey() {
       return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String getTraceUniqueKeyWithCachePrefix(String rawKey) {

        if (rawKey == null) {
            rawKey = "";
        }
        return TRACE_HTTP_REDIRECT_UNIQUE_KEY_PREFIX + rawKey;

    }

    public static Map<String, String> parseTraceContextFromJsonString(String traceContextJsonString) {
        try {
            if (StringUtils.isNotBlank(traceContextJsonString)) {
                Map<String, String> map = JSON.parseObject(traceContextJsonString, LinkedHashMap.class);
                return map;
            }
        } catch (Exception e) {
            logger.error("parseTraceContextFromString error", e);
        }

        return null;

    }

    public static String generateTraceContextJson(Map<String, String> traceContextMap) {

        if (MapUtils.isEmpty(traceContextMap)) {
            return null;
        }

        try {
            return JSON.toJSONString(traceContextMap);

        } catch (Exception e) {
            logger.error("generateTraceContextJson error", e);
        }

        return null;
    }

    public static void main(String[] args) {

        Map<String, String> traceContext = new LinkedHashMap<>();
        traceContext.put("traceId", "1234");
        List<Object> extra = new ArrayList<>();
        extra.add(1);
        extra.add("test");
        extra.add(new Long(99999999));
        traceContext.put("extra", JSON.toJSONString(extra));

        String traceContextJsonStr = generateTraceContextJson(traceContext);
        System.out.println(traceContextJsonStr);

        Map<String, String> map = parseTraceContextFromJsonString(traceContextJsonStr);
        String traceId = map.get("traceId");
        System.out.println(traceId);

        String extraString = map.get("extra");
        System.out.println(extraString);

    }

}
