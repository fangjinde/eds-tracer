package com.netease.edu.eds.trace.instrument.transaction.message;

/**
 * @author hzfjd
 * @create 18/10/22
 **/
public class TransactionMessageRestoreMethodContext {

    private static final ThreadLocal<String> RESOTRE_METHOD_CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setMethodName(String methodName) {
        RESOTRE_METHOD_CONTEXT_HOLDER.set(methodName);
    }

    public static boolean inRecheck() {
        String methodName = RESOTRE_METHOD_CONTEXT_HOLDER.get();
        return TransactionMessageConstants.RECHECK_METHOD_NAME.equals(methodName);
    }

    public static boolean inReconsume() {
        String methodName = RESOTRE_METHOD_CONTEXT_HOLDER.get();
        return TransactionMessageConstants.RECONSUME_METHOD_NAME.equals(methodName);
    }

    public static String getMethodName() {
        return RESOTRE_METHOD_CONTEXT_HOLDER.get();
    }

    public static boolean inRetoreInAll() {
        return inRecheck() || inReconsume();
    }

    public static void reset() {
        RESOTRE_METHOD_CONTEXT_HOLDER.remove();
    }
}
