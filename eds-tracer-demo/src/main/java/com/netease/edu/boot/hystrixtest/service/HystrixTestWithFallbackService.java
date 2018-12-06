package com.netease.edu.boot.hystrixtest.service;

/**
 * Created by hzfjd on 18/1/8.
 */
public interface HystrixTestWithFallbackService {
    String FALLBACK_PREFIX="fallback_";

    String echoWithFallbackSupport(Integer testCase);

    String echoWithoutFallbackSupport(Integer testCase);

}
