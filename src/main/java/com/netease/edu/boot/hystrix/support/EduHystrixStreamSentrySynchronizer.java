package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/18.
 */

import com.alibaba.fastjson.JSON;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hzfjd
 * @create 18/1/18
 */
public class EduHystrixStreamSentrySynchronizer implements SmartLifecycle {

    private static Logger logger = LoggerFactory.getLogger(EduHystrixStreamSentrySynchronizer.class);

    private AtomicBoolean running = new AtomicBoolean(false);

    private Subscription sampleSubscription = null;

    private Observable<HystrixDashboardStream.DashboardData> sampleStream = null;

    @Override
    public void start() {

        if (running.get()) {
            return;
        }

        logger.info("EduHystrixStreamSentrySynchronizer starting...");

        sampleStream = EduHystrixDashboardStream.getInstance().observe();
        sampleSubscription = sampleStream
                .observeOn(Schedulers.immediate())
                .subscribe(new Subscriber<HystrixDashboardStream.DashboardData>() {

                    @Override
                    public void onCompleted() {
                        logger.error(
                                "EduHystrixStreamSentrySynchronizer: ({}) received unexpected OnCompleted from sample stream",
                                getClass().getSimpleName());
                    }

                    @Override
                    public void onError(Throwable e) {
                        logger.error(
                                "EduHystrixStreamSentrySynchronizer: ({}) received unexpected onError from sample stream",
                                getClass().getSimpleName(), e);
                    }

                    @Override
                    public void onNext(HystrixDashboardStream.DashboardData sampleData) {
                        try {
                            HystrixMetricsSentryCollector.onNext(sampleData);
                        } catch (RuntimeException e) {
                            logger.error(String.format("HystrixMetricsSentryCollector.onNext Error,with sampleData:%s",
                                                       JSON.toJSONString(sampleData)), e);
                        }

                    }
                });

        running.compareAndSet(false, true);
        logger.info(String.format("EduHystrixStreamSentrySynchronizer started. sampleSubscription:%s",
                                  !sampleSubscription.isUnsubscribed()));
    }

    @Override
    public void stop() {
        innnerStop();
    }

    private void innnerStop() {
        logger.info("EduHystrixStreamSentrySynchronizer stoping...");
        if (sampleSubscription != null && !sampleSubscription.isUnsubscribed()) {
            sampleSubscription.unsubscribe();
        }
        running.compareAndSet(true, false);
        logger.info(String.format("EduHystrixStreamSentrySynchronizer stopped. sampleSubscription:%s",
                                  !sampleSubscription.isUnsubscribed())
        );
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        innnerStop();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
