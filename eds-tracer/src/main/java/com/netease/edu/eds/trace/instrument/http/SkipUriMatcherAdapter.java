package com.netease.edu.eds.trace.instrument.http;

/**
 * @author hzfjd
 * @create 19/1/28
 **/
public class SkipUriMatcherAdapter implements SkipUriMatcher {

    private com.netease.edu.eds.trace.springbootcompatible.spi.SkipUriMatcher adaptee;

    public SkipUriMatcherAdapter(com.netease.edu.eds.trace.springbootcompatible.spi.SkipUriMatcher skipUriMatcher) {
        this.adaptee = skipUriMatcher;
    }

    @Override
    public boolean match(String uri) {
        if (adaptee == null) {
            return false;
        }
        return adaptee.match(uri);
    }
}
