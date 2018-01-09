package com.netease.edu.boot.hystrixtest.service;/**
 * Created by hzfjd on 18/1/8.
 */

import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/1/8
 */

@Component("hystrixScratchService")
public class HystrixScratchServiceImpl implements HystrixScratchService {

    @Override
    public String echo(Integer testCase) {
        if (testCase==null){
            return "normal null";
        }else if(testCase==0){
            return testCase.toString();
        }else if (testCase==1){
            throw new FrontNotifiableRuntimeException("i should be ignored by hystrix, and pass to downstream");
        }else if (testCase==2){
            throw new SystemErrorRuntimeException("i should be record by hystrix, and trigger fallback if it exists");
        }

        return testCase.toString();
    }
}
