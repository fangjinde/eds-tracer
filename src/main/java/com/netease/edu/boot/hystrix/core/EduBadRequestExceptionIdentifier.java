package com.netease.edu.boot.hystrix.core;

import com.netease.edu.boot.hystrix.core.constants.HystrixBeanNameContants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Created by hzfjd on 18/1/10.
 */
public class EduBadRequestExceptionIdentifier {

    HystrixIgnoreExceptionProvider ignorableExceptions;
    HystrixIgnoreExceptionProvider ignorableSuperExceptions;

    @Autowired(required = false)
    @Qualifier(HystrixBeanNameContants.EDU_HYSTRIX_IGNORABLE_EXCEPTIONS_BEAN_NAME)
    public void setIgnorableExceptions(HystrixIgnoreExceptionProvider ignorableExceptions) {
        this.ignorableExceptions = ignorableExceptions;
        if (s_ignorableExceptions == null) {
            synchronized (EduBadRequestExceptionIdentifier.class){
                if (s_ignorableExceptions==null){
                    s_ignorableExceptions=ignorableExceptions.getIgnorable();
                }
            }
        }
    }

    @Autowired(required = false)
    @Qualifier(HystrixBeanNameContants.EDU_HYSTRIX_IGNORABLE_SUPER_EXCEPTIONS_BEAN_NAME)
    public void setIgnorableSuperExceptions(HystrixIgnoreExceptionProvider ignorableSuperExceptions) {
        this.ignorableSuperExceptions = ignorableSuperExceptions;
        if (s_ignorableSuperExceptions==null){
            synchronized (EduBadRequestExceptionIdentifier.class){
                if (s_ignorableSuperExceptions==null){
                    s_ignorableSuperExceptions=ignorableSuperExceptions.getIgnorable();
                }
            }
        }
    }

    private static Class<? extends Throwable>[] s_ignorableExceptions;
    private static Class<? extends Throwable>[] s_ignorableSuperExceptions;

    public static boolean isIgnorable(Throwable e) {
        if (s_ignorableExceptions!=null){
            for (Class ignorable : s_ignorableExceptions) {
                if (ignorable.equals(e.getClass())) {
                    return true;
                }
            }
        }
        if (s_ignorableSuperExceptions!=null){
            for (Class ignorableSuper : s_ignorableSuperExceptions) {
                if (ignorableSuper.isAssignableFrom(e.getClass())) {
                    return true;
                }
            }
        }
        return false;
    }

}
