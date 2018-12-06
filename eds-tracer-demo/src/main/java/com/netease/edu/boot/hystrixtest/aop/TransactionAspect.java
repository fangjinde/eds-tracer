package com.netease.edu.boot.hystrixtest.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

import com.netease.edu.persist.service.aop.AbstractEnhancedTransactionAspect;

@Aspect
public class TransactionAspect extends AbstractEnhancedTransactionAspect implements Ordered {

    private int order;

    @Override
    @Pointcut("execution(* com.netease.edu..*.service.*.add*(..))"
              + "||execution(* com.netease.edu..*.service.*.insert*(..))"
              + "||execution(* com.netease.edu..*.service.*.create*(..))"
              + "||execution(* com.netease.edu..*.service.*.update*(..))"
              + "||execution(* com.netease.edu..*.service.*.modify*(..))"
              + "||execution(* com.netease.edu..*.service.*.delete*(..))"
              + "||execution(* com.netease.edu..*.service.*.remove*(..))"
              + "||execution(* com.netease.edu..*.logic.*.*WithTransaction(..))"
              + "||execution(@com.netease.edu.persist.transaction.annotation.EduTransaction * *(..))")
    public void transaction() {
    }

    @Override
    @Around("transaction()")
    public Object aroundTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        return super.aroundTransaction(joinPoint);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void setOrder(int order) {
        this.order = order;
    }

}
