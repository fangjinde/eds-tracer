package com.netease.edu.boot.hystrix.support;

import com.netease.edu.boot.hystrix.core.OriginApplicationNameResolver;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * @author hzfjd
 * @create 17/12/24
 */
public class OriginApplicationNameControllerResolver implements OriginApplicationNameResolver {

    @Autowired
    private HttpServletRequest request;

    @Override
    public String getOriginApplicationName() {
       return request.getHeader(OriginApplicationConstants.HEADER_NAME);
    }
}
