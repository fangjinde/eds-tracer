package com.netease.edu.eds.shuffle.instrument.rabbit;

import org.springframework.amqp.core.Message;

/**
 * @author hzfjd
 * @create 18/8/24
 **/
public class RabbitShuffleSendContext {

    private static ThreadLocal<Boolean>          ignoreShuffleThreadLocal    = new ThreadLocal<>();
    private static ThreadLocal<DelaySendContext> delaySendContextThreadLocal = new ThreadLocal<>();

    public static void ignoreShuffle() {
        ignoreShuffleThreadLocal.set(true);
    }

    public static boolean shouldIgnoreShuffle() {
        Boolean ingoreShuffle = ignoreShuffleThreadLocal.get();
        if (ingoreShuffle != null && ingoreShuffle.booleanValue()) {
            return true;
        }
        return false;
    }

    public static void reset() {
        ignoreShuffleThreadLocal.remove();
    }

    public static ThreadLocal<DelaySendContext> getDelaySendContextThreadLocal() {
        return delaySendContextThreadLocal;
    }

    public static class DelaySendContext {

        public DelaySendContext(Message message) {
            this.message = message;
        }

        private Message message;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

}
