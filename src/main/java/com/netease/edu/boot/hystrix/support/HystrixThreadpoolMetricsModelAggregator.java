package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netease.edu.boot.hystrix.core.HystrixKeyParam;
import com.netease.sentry.javaagent.collector.api.MultiPrimaryKeyAggregator;
import com.netease.sentry.javaagent.collector.api.PrimaryKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixThreadpoolMetricsModelAggregator
        extends MultiPrimaryKeyAggregator<HystrixThreadpoolMetricsModelAggregator.HystrixThreadpoolMetricsSentryHolder> {

    private String modelName;

    public HystrixThreadpoolMetricsModelAggregator(String modelName) {
        this.modelName = modelName;
    }

    @Override
    protected int primaryKeyLength() {
        return 2;
    }

    @Override
    protected Map<String, Object> constructItemRow(PrimaryKey primaryKey,
                                                   HystrixThreadpoolMetricsSentryHolder hystrixThreadpoolMetricsSentryHolder) {

        HystrixThreadPoolMetrics hystrixThreadPoolMetrics = hystrixThreadpoolMetricsSentryHolder.getHystrixThreadPoolMetrics();
        if (hystrixThreadPoolMetrics == null) {
            return null;
        }
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("Thread", primaryKey.get(0));
        row.put("Origin", primaryKey.get(1));
        // total
        row.put("Active", hystrixThreadPoolMetrics.getCurrentActiveCount());
        row.put("CorePool", hystrixThreadPoolMetrics.getCurrentCorePoolSize());
        row.put("Queue", hystrixThreadPoolMetrics.getCurrentQueueSize());
        row.put("Pool", hystrixThreadPoolMetrics.getCurrentPoolSize());
        row.put("Task", hystrixThreadPoolMetrics.getCurrentTaskCount());
        row.put("CompletedTask", hystrixThreadPoolMetrics.getCurrentCompletedTaskCount());
        row.put("LargestPool", hystrixThreadPoolMetrics.getCurrentLargestPoolSize());
        row.put("MaxPool", hystrixThreadPoolMetrics.getCurrentMaximumPoolSize());
        // concurrency
        return row;
    }

    @Override
    protected Class getValueType() {
        return HystrixThreadpoolMetricsSentryHolder.class;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    private String getOriginApplicationNameWithDefault(String originApplicationName) {
        if (originApplicationName == null || originApplicationName.length() == 0) {
            return "none";
        }
        return originApplicationName;
    }

    public void updateMetrics(HystrixThreadPoolMetrics data, HystrixKeyParam hystrixKeyParam) {
        //永远记录采样点的那个数据
        getValue(hystrixKeyParam.generateByPrefixAndMethodSignature(), getOriginApplicationNameWithDefault(
                hystrixKeyParam.getOriginApplicationName())).setHystrixThreadPoolMetrics(
                data);
    }

    public static class HystrixThreadpoolMetricsSentryHolder {

        AtomicReference<HystrixThreadPoolMetrics> hystrixThreadPoolMetricsHolder;

        public HystrixThreadPoolMetrics getHystrixThreadPoolMetrics() {
            return hystrixThreadPoolMetricsHolder.get();
        }

        public void setHystrixThreadPoolMetrics(HystrixThreadPoolMetrics hystrixThreadPoolMetrics) {
            hystrixThreadPoolMetricsHolder.set(hystrixThreadPoolMetrics);
        }
    }
}
