package com.netease.edu.eds.trace.instrument.http;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hzfjd on 18/4/3.
 */
public interface WebUserMatcher {
    public boolean matches(HttpServletRequest request,String loginIdCriteria,Integer loginTypeCriteria);
}
