package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.instrument.http.ClientSkipUriMatcher;

import java.util.regex.Pattern;

/**
 * @author hzfjd
 * @create 19/2/18
 **/
public class ClientSkipUriMatcherRegexImpl extends SkipUriMatcherRegexImpl implements ClientSkipUriMatcher {

    public ClientSkipUriMatcherRegexImpl(Pattern pattern) {
        super(pattern);
    }
}
