package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.instrument.http.ServerSkipUriMatcher;

import java.util.regex.Pattern;

/**
 * @author hzfjd
 * @create 19/2/18
 **/
public class ServerSkipUriMatcherRegexImpl extends SkipUriMatcherRegexImpl implements ServerSkipUriMatcher {

    public ServerSkipUriMatcherRegexImpl(Pattern pattern) {
        super(pattern);
    }
}
