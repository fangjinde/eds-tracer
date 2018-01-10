package com.netease.edu.boot.hystrix.core;

import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;

/**
 * Created by hzfjd on 18/1/10.
 */
public interface CommandAction<R> {
    R execute() throws CommandExecuteException;
}
