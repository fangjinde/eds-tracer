package com.netease.edu.eds.trace.springbootcompatible.utils;

import org.springframework.cloud.sleuth.instrument.web.SleuthWebProperties;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * @author hzfjd
 * @create 19/1/28
 **/
public class SkipPatternUtils {

    public static Pattern defaultSkipPattern(String skipPattern, String additionalSkipPattern) {
        return Pattern.compile(combinedPattern(skipPattern, additionalSkipPattern));
    }

    public static String combinedPattern(String skipPattern, String additionalSkipPattern) {
        String pattern = skipPattern;
        if (!StringUtils.hasText(skipPattern)) {
            pattern = SleuthWebProperties.DEFAULT_SKIP_PATTERN;
        }
        if (StringUtils.hasText(additionalSkipPattern)) {
            return pattern + "|" + additionalSkipPattern;
        }
        return pattern;
    }
}
