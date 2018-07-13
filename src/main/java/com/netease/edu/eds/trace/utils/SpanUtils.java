package com.netease.edu.eds.trace.utils;/**
                                         * Created by hzfjd on 18/3/27.
                                         */

import brave.Span;
import brave.SpanCustomizer;

/**
 * @author hzfjd
 * @create 18/3/27
 */
public class SpanUtils {

    private static final int MAX_SPAN_NAME_LENGTH = 128;

    private static final int MAX_SPAN_TAG_LENGTH  = 128;

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

    public static void safeTag(Span span, String name, String value) {
        if (span != null && !span.isNoop() && name != null && name.length() > 0 && value != null) {
            span.tag(name, value);
        }
    }

    public static void safeTag(SpanCustomizer span, String name, String value) {
        if (span != null && name != null && name.length() > 0 && value != null) {
            span.tag(name, value);
        }
    }

}
