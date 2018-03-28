package com.netease.edu.eds.trace.utils;/**
 * Created by hzfjd on 18/3/27.
 */

/**
 * @author hzfjd
 * @create 18/3/27
 */
public class SpanStringUtils {

    private static final int MAX_SPAN_NAME_LENGTH = 128;

    private static final int MAX_SPAN_TAG_LENGTH = 128;

    public static String filterSpanName(String spanName) {
        if (spanName == null) {
            return "none";
        }

        if (spanName.length() > MAX_SPAN_NAME_LENGTH) {
            spanName = spanName.substring(0, MAX_SPAN_NAME_LENGTH);
        }

        return spanName;

    }

    public static String filterSpanTag(String tag) {

        if (tag != null && tag.length() > MAX_SPAN_TAG_LENGTH) {
            return tag.substring(0, MAX_SPAN_TAG_LENGTH);
        }

        return tag;
    }

}
