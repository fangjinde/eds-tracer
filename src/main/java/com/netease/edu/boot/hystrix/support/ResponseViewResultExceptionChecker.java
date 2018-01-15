package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/15.
 */

import com.netease.edu.boot.hystrix.core.ResultExceptionChecker;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import com.netease.edu.web.viewer.ResponseView;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class ResponseViewResultExceptionChecker implements ResultExceptionChecker<Object> {

    @Override
    public void check(Object result) throws CommandExecuteException {
        if (result instanceof ResponseView){
            ResponseView rv=(ResponseView)result;
            if (rv.getCode()==ResponseView.EXCEPTION_IGNORE){
                throw new CommandExecuteException("system error detected by ResponseViewResultExceptionChecker").withResult(result);
            }
        }
    }
}
