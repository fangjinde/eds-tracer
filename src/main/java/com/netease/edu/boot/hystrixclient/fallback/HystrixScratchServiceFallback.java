package com.netease.edu.boot.hystrixclient.fallback;/**
 * Created by hzfjd on 18/1/9.
 */

import com.netease.edu.boot.hystrix.annotation.EduFallbackBean;
import com.netease.edu.boot.hystrix.core.HystrixExecutionContext;
import com.netease.edu.boot.hystrixtest.service.HystrixScratchService;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/9
 */
@EduFallbackBean
@Component
public class HystrixScratchServiceFallback implements HystrixScratchService {

    @Override
    public String echo(Integer testCase) {
        Throwable e = HystrixExecutionContext.getExecutionException();
        return "calling with args: " + testCase + ", encounter exception of: " + e.getMessage() + " , fallback. ";
    }
}
