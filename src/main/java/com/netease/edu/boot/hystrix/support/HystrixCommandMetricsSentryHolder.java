package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netflix.hystrix.HystrixCommandMetrics;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixCommandMetricsSentryHolder {

    AtomicReference<HystrixCommandMetrics> hystrixCommandMetricsHolder;

    public HystrixCommandMetrics getHystrixCommandMetrics() {
        return hystrixCommandMetricsHolder.get();
    }

    public void setHystrixCommandMetrics(HystrixCommandMetrics hystrixCommandMetrics) {
        hystrixCommandMetricsHolder.set(hystrixCommandMetrics);
    }
}
