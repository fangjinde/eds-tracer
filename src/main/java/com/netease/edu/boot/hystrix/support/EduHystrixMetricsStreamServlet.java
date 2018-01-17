package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.contrib.sample.stream.HystrixSampleSseServlet;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class EduHystrixMetricsStreamServlet extends HystrixSampleSseServlet {

    private static final long serialVersionUID = -7548505095303313237L;

    /* used to track number of connections and throttle */
    private static AtomicInteger      concurrentConnections    = new AtomicInteger(0);
    private static DynamicIntProperty maxConcurrentConnections =
            DynamicPropertyFactory.getInstance().getIntProperty("hystrix.config.stream.maxConcurrentConnections", 5);

    public EduHystrixMetricsStreamServlet() {
        this(HystrixDashboardStream.getInstance().observe(), DEFAULT_PAUSE_POLLER_THREAD_DELAY_IN_MS);
    }

    /* package-private */ EduHystrixMetricsStreamServlet(Observable<HystrixDashboardStream.DashboardData> sampleStream,
                                                      int pausePollerThreadDelayInMs) {
        super(sampleStream.concatMap(new Func1<HystrixDashboardStream.DashboardData, Observable<String>>() {

            @Override
            public Observable<String> call(HystrixDashboardStream.DashboardData dashboardData) {
                //return Observable.from(com.netflix.hystrix.serial.SerialHystrixDashboardData.toMultipleJsonStrings(dashboardData));
                //FIXME
                return null;
            }
        }), pausePollerThreadDelayInMs);
    }

    @Override
    protected int getMaxNumberConcurrentConnectionsAllowed() {
        return maxConcurrentConnections.get();
    }

    @Override
    protected int getNumberCurrentConnections() {
        return concurrentConnections.get();
    }

    @Override
    protected int incrementAndGetCurrentConcurrentConnections() {
        return concurrentConnections.incrementAndGet();
    }

    @Override
    protected void decrementCurrentConcurrentConnections() {
        concurrentConnections.decrementAndGet();
    }
}
