package com.netease.edu.eds.trace.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/9/10
 **/
public class TraceContextPropagationUtils {

    private static final Logger logger = LoggerFactory.getLogger(TraceContextPropagationUtils.class);

    public static Map<String, String> parseTraceContextFromString(String traceContextHexString) {
        try {
            if (StringUtils.isNotBlank(traceContextHexString)) {
                String json = HexUtils.hexStringToBytesStr(traceContextHexString);
                if (StringUtils.isNotBlank(json)) {
                    Map<String, String> map = JSON.parseObject(json, LinkedHashMap.class);
                    return map;
                }
            }
        } catch (Exception e) {
            logger.error("parseTraceContextFromString error", e);
        }

        return null;

    }

    public static String getTraceContextValue(String traceContextHexString, String key) {
        Map<String, String> map = parseTraceContextFromString(traceContextHexString);
        if (map != null) {
            String value = map.get(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;

    }

    public static String generateTraceContextHexString(Map<String, String> traceContextMap) {

        if (MapUtils.isEmpty(traceContextMap)) {
            return null;
        }

        try {
            String json = JSON.toJSONString(traceContextMap);
            if (StringUtils.isBlank(json)) {
                return null;
            }
            String jsonHex = HexUtils.bytesStrToHexStr(json);
            if (StringUtils.isNotBlank(jsonHex)) {
                return jsonHex;
            }
        } catch (Exception e) {
            logger.error("generateTraceContextHexString error", e);
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

        String traceContextHexStr = generateTraceContextHexString(traceContext);
        System.out.println(traceContextHexStr);

        String traceId = getTraceContextValue(traceContextHexStr, "traceId");
        System.out.println(traceId);

        String extraString = getTraceContextValue(traceContextHexStr, "extra");
        System.out.println(extraString);

    }

}
