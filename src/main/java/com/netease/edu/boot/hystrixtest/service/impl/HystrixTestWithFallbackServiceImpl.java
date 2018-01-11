package com.netease.edu.boot.hystrixtest.service.impl;/**
 * Created by hzfjd on 18/1/8.
 */

import com.netease.edu.boot.hystrixtest.service.HystrixTestWithFallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/8
 */

@Component("hystrixTestWithFallbackService")
public class HystrixTestWithFallbackServiceImpl implements HystrixTestWithFallbackService {

    @Autowired
    EchoLogic echoLogic;


    @Override
    public String echoWithFallbackSupport(Integer testCase) {
        return echoLogic.innerEcho(testCase);
    }

    @Override
    public String echoWithoutFallbackSupport(Integer testCase) {
        return echoLogic.innerEcho(testCase);
    }
}
