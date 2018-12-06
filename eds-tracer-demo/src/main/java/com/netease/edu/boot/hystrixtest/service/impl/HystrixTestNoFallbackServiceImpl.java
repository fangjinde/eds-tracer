package com.netease.edu.boot.hystrixtest.service.impl;/**
 * Created by hzfjd on 18/1/11.
 */

import com.netease.edu.boot.hystrixtest.service.HystrixTestNoFallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/11
 */
@Component("hystrixTestNoFallbackService")
public class HystrixTestNoFallbackServiceImpl implements HystrixTestNoFallbackService {

    @Autowired
    EchoLogic echoLogic;


    @Override
    public String echo(Integer testCase) {
        return echoLogic.innerEcho(testCase);
    }
}
