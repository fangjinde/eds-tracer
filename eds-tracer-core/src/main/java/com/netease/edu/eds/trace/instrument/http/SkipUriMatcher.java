package com.netease.edu.eds.trace.instrument.http;

/**
 * Created by hzfjd on 18/3/30.
 */
public interface SkipUriMatcher {

	boolean match(String uri);

}
