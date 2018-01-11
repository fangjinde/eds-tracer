package com.netease.edu.boot.hystrixtest.service.impl;/**
 * Created by hzfjd on 18/1/11.
 */

import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/11
 */
@Component
public class EchoLogic {
    public String innerEcho(Integer testCase) {
        if (testCase==null){
            return "null";
        }else if (testCase % 10 == 1){
            throw new FrontNotifiableRuntimeException("i should be ignored by hystrix, and pass to downstream");
        }else if (testCase % 10 ==2){
            throw new SystemErrorRuntimeException("i should be record by hystrix, and trigger fallback if it exists");
        }else if (testCase % 10==3){
            throw new BeanCreationException(" i should be record by hystrix , but wrapper by dubbo");
        }

        return testCase.toString();
    }
}
