package com.netease.edu.eds.trace.instrument.http;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * @author hzfjd
 * @create 18/11/26
 **/
public class RediectUrlTraceMatcherRegexImpl implements RedirectUrlTraceMatcher {

    private Pattern pattern;

    public RediectUrlTraceMatcherRegexImpl(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean needTrace(URL url) {

        if (url == null) {
            return false;
        }

        if (pattern == null) {
            return false;
        }

        if (pattern.matcher(url.getHost()).find()) {
            return true;
        }

        return false;
    }

}
