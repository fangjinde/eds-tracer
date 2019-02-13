package com.netease.edu.eds.trace.instrument.http;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hzfjd on 18/4/2.
 */
public interface WebDebugMatcher {

    boolean matches(HttpServletRequest request);

    String debugMark();
}
