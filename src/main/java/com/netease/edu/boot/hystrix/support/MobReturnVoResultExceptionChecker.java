package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/15.
 */

import com.netease.edu.agent.mobile.common.vo.MobReturnVo;
import com.netease.edu.boot.hystrix.core.ResultExceptionChecker;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class MobReturnVoResultExceptionChecker implements ResultExceptionChecker {

    @Override
    public void check(Object result) throws CommandExecuteException {
        if (result instanceof MobReturnVo){
            MobReturnVo mobReturnVo=(MobReturnVo)result;
            if (MobReturnVo.MOBILE_SYSTEM_ERROR_CODE.equals(mobReturnVo.getStatus().get("code"))){
                throw new CommandExecuteException("system exception detected by MobReturnVoResultExceptionChecker").withResult(result);
            }
        }

    }
}
