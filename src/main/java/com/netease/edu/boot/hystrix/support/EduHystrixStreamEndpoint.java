package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import org.springframework.cloud.netflix.endpoint.ServletWrappingEndpoint;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class EduHystrixStreamEndpoint extends ServletWrappingEndpoint {

    public EduHystrixStreamEndpoint() {
        super(EduHystrixMetricsStreamServlet.class, "hystrixStream", "/hystrix.stream",
              false, true);
    }
}
