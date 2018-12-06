package com.netease.edu.boot.hystrixclient.fallback;/**
 * Created by hzfjd on 18/1/9.
 */

import com.netease.edu.boot.hystrix.annotation.EduFallbackBean;
import com.netease.edu.boot.hystrixtest.service.HystrixTestWithFallbackService;
import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/9
 */
@EduFallbackBean
@Component
public class HystrixScratchServiceFallback implements HystrixTestWithFallbackService {

    @Override
    public String echoWithFallbackSupport(Integer testCase) {
        if (testCase<10){
            return FALLBACK_PREFIX+testCase;
        }
        if (testCase ==12){
            throw new FrontNotifiableRuntimeException("exception in fallback");
        }else if (testCase ==13){
            throw new SystemErrorRuntimeException("exception in fallback");
        }
        throw new BeanCreationException("exception in fallback");
    }

    @Override
    public String echoWithoutFallbackSupport(Integer testCase) {
        throw new UnsupportedOperationException();
    }
}
