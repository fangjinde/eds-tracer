package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/10.
 */

import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;

/**
 * @author hzfjd
 * @create 18/1/10
 */
public interface ResultExceptionChecker<R> {
    void check(R result) throws CommandExecuteException;
}
