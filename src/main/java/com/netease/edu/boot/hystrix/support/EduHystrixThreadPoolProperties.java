package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/9.
 */

import com.netease.edu.boot.hystrix.core.HystrixKeyParam;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesChainedProperty;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import org.apache.commons.lang.StringUtils;

import static com.netflix.hystrix.strategy.properties.HystrixPropertiesChainedProperty.forInteger;

/**
 * 增加属性查找fallback.
 * 1. 完整key查询,包括SidePrefix,originApplicationName,MethodSignature
 * 2. 增加忽略originApplicationName的CommandKey属性查询
 * 3. 增加包含SidePrefix的default Key的属性查询
 * 4. 原default查询
 * @author hzfjd
 * @create 18/1/9
 */
public class EduHystrixThreadPoolProperties extends HystrixThreadPoolProperties {

    /* defaults */
    private Integer default_coreSize                                        = 10; // size of thread pool
    private Integer default_keepAliveTimeMinutes                            = 1; // minutes to keep a thread alive (though in practice this doesn't get used as by default we set a fixed size)
    private Integer default_maxQueueSize                                    = -1; // size of queue (this can't be dynamically changed so we use 'queueSizeRejectionThreshold' to artificially limit and reject)
    // -1 turns if off and makes us use SynchronousQueue
    private Integer default_queueSizeRejectionThreshold                     = 5; // number of items in queue
    private Integer default_threadPoolRollingNumberStatisticalWindow        = 10000; // milliseconds for rolling number
    private Integer default_threadPoolRollingNumberStatisticalWindowBuckets = 10; // number of buckets in rolling number (10 1-second buckets)

    private final HystrixProperty<Integer> corePoolSize;
    private final HystrixProperty<Integer> keepAliveTime;
    private final HystrixProperty<Integer> maxQueueSize;
    private final HystrixProperty<Integer> queueSizeRejectionThreshold;
    private final HystrixProperty<Integer> threadPoolRollingNumberStatisticalWindowInMilliseconds;
    private final HystrixProperty<Integer> threadPoolRollingNumberStatisticalWindowBuckets;

    protected EduHystrixThreadPoolProperties(HystrixThreadPoolKey key) {
        this(key, HystrixThreadPoolProperties.Setter(), "hystrix");
    }

    protected EduHystrixThreadPoolProperties(HystrixThreadPoolKey key, Setter builder) {
        this(key, builder, "hystrix");
    }

    protected EduHystrixThreadPoolProperties(HystrixThreadPoolKey key, Setter builder, String propertyPrefix) {
        //it will be waste, but i have no other idea.
        super(key, builder, propertyPrefix);
        //re init for EduHystrixThreadPoolProperties instance
        this.corePoolSize = getProperty(propertyPrefix, key, "coreSize", builder.getCoreSize(), default_coreSize);
        this.keepAliveTime = getProperty(propertyPrefix, key, "keepAliveTimeMinutes", builder.getKeepAliveTimeMinutes(),
                                         default_keepAliveTimeMinutes);
        this.maxQueueSize = getProperty(propertyPrefix, key, "maxQueueSize", builder.getMaxQueueSize(),
                                        default_maxQueueSize);
        this.queueSizeRejectionThreshold = getProperty(propertyPrefix, key, "queueSizeRejectionThreshold",
                                                       builder.getQueueSizeRejectionThreshold(),
                                                       default_queueSizeRejectionThreshold);
        this.threadPoolRollingNumberStatisticalWindowInMilliseconds = getProperty(propertyPrefix, key,
                                                                                  "metrics.rollingStats.timeInMilliseconds",
                                                                                  builder.getMetricsRollingStatisticalWindowInMilliseconds(),
                                                                                  default_threadPoolRollingNumberStatisticalWindow);
        this.threadPoolRollingNumberStatisticalWindowBuckets = getProperty(propertyPrefix, key,
                                                                           "metrics.rollingStats.numBuckets",
                                                                           builder.getMetricsRollingStatisticalWindowBuckets(),
                                                                           default_threadPoolRollingNumberStatisticalWindowBuckets);
    }

    private static HystrixProperty<Integer> getProperty(String propertyPrefix, HystrixThreadPoolKey key,
                                                        String instanceProperty, Integer builderOverrideValue,
                                                        Integer defaultValue) {

        HystrixKeyParam hystrixKeyParam = HystrixKeyParam.parseFromKey(key.name());
        String commandKey = hystrixKeyParam.generateCommandKey();
        String sidePrefix = hystrixKeyParam.getSidePrefix();

        HystrixPropertiesChainedProperty.ChainBuilder<Integer> cb = forInteger()
                .add(propertyPrefix + ".threadpool." + key.name() + "." + instanceProperty, builderOverrideValue);

        if (!key.name().equals(commandKey)) {
            cb.add(propertyPrefix + ".threadpool." + commandKey + "." + instanceProperty, builderOverrideValue);
        }
        if (StringUtils.isNotBlank(sidePrefix)) {
            cb.add(propertyPrefix + ".threadpool." + sidePrefix + ".default." + instanceProperty, defaultValue);
        }

        cb.add(propertyPrefix + ".threadpool.default." + instanceProperty, defaultValue);

        return cb.build();
    }

    /**
     * Core thread-pool size that gets passed to {@link java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)}
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> coreSize() {
        return corePoolSize;
    }

    /**
     * Keep-alive time in minutes that gets passed to {@link java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)}
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> keepAliveTimeMinutes() {
        return keepAliveTime;
    }

    /**
     * Max queue size that gets passed to {@link java.util.concurrent.BlockingQueue} in {@link com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy#getBlockingQueue(int)}
     * This should only affect the instantiation of a threadpool - it is not eliglible to change a queue size on the fly.
     * For that, use {@link #queueSizeRejectionThreshold()}.
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> maxQueueSize() {
        return maxQueueSize;
    }

    /**
     * Queue size rejection threshold is an artificial "max" size at which rejections will occur even if {@link #maxQueueSize} has not been reached. This is done because the {@link #maxQueueSize} of a
     * {@link java.util.concurrent.BlockingQueue} can not be dynamically changed and we want to support dynamically changing the queue size that affects rejections.
     * This is used by {@link com.netflix.hystrix.HystrixCommand} when queuing a thread for execution.
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> queueSizeRejectionThreshold() {
        return queueSizeRejectionThreshold;
    }

    /**
     * Duration of statistical rolling window in milliseconds. This is passed into {@link com.netflix.hystrix.util.HystrixRollingNumber} inside each {@link com.netflix.hystrix.HystrixThreadPoolMetrics} instance.
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds() {
        return threadPoolRollingNumberStatisticalWindowInMilliseconds;
    }

    /**
     * Number of buckets the rolling statistical window is broken into. This is passed into {@link com.netflix.hystrix.util.HystrixRollingNumber} inside each {@link com.netflix.hystrix.HystrixThreadPoolMetrics} instance.
     *
     * @return {@code HystrixProperty<Integer>}
     */
    public HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets() {
        return threadPoolRollingNumberStatisticalWindowBuckets;
    }

}
