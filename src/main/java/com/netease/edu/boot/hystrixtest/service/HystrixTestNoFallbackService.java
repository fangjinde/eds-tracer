package com.netease.edu.boot.hystrixtest.service;/**
 * Created by hzfjd on 18/1/11.
 */

/**
 * @author hzfjd
 * @create 18/1/11
 */
public interface HystrixTestNoFallbackService {
    String echo(Integer testCase);
}
