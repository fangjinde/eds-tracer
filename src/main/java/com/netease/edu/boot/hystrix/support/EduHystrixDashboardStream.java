package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesChainedProperty;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class EduHystrixDashboardStream {

    final int                                              delayInMs;
    final Observable<HystrixDashboardStream.DashboardData> singleSource;
    final AtomicBoolean isSourceCurrentlySubscribed = new AtomicBoolean(false);

    //使用Hystrix的动态配置
    private static final HystrixProperty<Integer> dataEmissionIntervalInMs = HystrixPropertiesChainedProperty.forInteger().add(
            "hystrix.stream.dashboard.intervalInMilliseconds", 500).build();

    private EduHystrixDashboardStream(int delayInMs) {
        this.delayInMs = delayInMs;
        this.singleSource = Observable.interval(delayInMs, TimeUnit.MILLISECONDS)
                                      .map(new Func1<Long, HystrixDashboardStream.DashboardData>() {

                                          @Override
                                          public HystrixDashboardStream.DashboardData call(Long timestamp) {
                                              return new HystrixDashboardStream.DashboardData(
                                                      HystrixCommandMetrics.getInstances(),
                                                      HystrixThreadPoolMetrics.getInstances(),
                                                      HystrixCollapserMetrics.getInstances()
                                              );
                                          }
                                      })
                                      .doOnSubscribe(new Action0() {
                                          @Override
                                          public void call() {
                                              isSourceCurrentlySubscribed.set(true);
                                          }
                                      })
                                      .doOnUnsubscribe(new Action0() {
                                          @Override
                                          public void call() {
                                              isSourceCurrentlySubscribed.set(false);
                                          }
                                      })
                                      .share()
                                      .onBackpressureDrop();
    }

    //The data emission interval is looked up on startup only
    private static final EduHystrixDashboardStream INSTANCE =
            new EduHystrixDashboardStream(dataEmissionIntervalInMs.get());

    public static EduHystrixDashboardStream getInstance() {
        return INSTANCE;
    }

    static EduHystrixDashboardStream getNonSingletonInstanceOnlyUsedInUnitTests(int delayInMs) {
        return new EduHystrixDashboardStream(delayInMs);
    }

    /**
     * Return a ref-counted stream that will only do work when at least one subscriber is present
     */
    public Observable<HystrixDashboardStream.DashboardData> observe() {
        return singleSource;
    }

    public boolean isSourceCurrentlySubscribed() {
        return isSourceCurrentlySubscribed.get();
    }


}
