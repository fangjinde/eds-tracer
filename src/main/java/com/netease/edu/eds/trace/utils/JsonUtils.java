package com.netease.edu.eds.trace.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author hzfjd
 * @create 18/5/25
 **/
public class JsonUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger       = LoggerFactory.getLogger(JsonUtils.class);

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("toJson error", e);
            return null;
        }

    }
}
