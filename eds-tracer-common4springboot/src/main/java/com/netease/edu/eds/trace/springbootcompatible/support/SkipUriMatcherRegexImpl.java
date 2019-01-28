package com.netease.edu.eds.trace.springbootcompatible.support;/**
 * Created by hzfjd on 18/3/30.
 */

import com.netease.edu.eds.trace.springbootcompatible.spi.SkipUriMatcher;

import java.util.regex.Pattern;

/**
 * @author hzfjd
 * @create 18/3/30
 */
public class SkipUriMatcherRegexImpl implements SkipUriMatcher {

    private Pattern pattern;

    public SkipUriMatcherRegexImpl(Pattern pattern) {
        this.pattern = pattern;
    }

    public boolean match(String uri) {
        return pattern.matcher(uri).find();
    }
}
