package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/15.
 */

import com.netease.edu.boot.hystrix.core.ResultExceptionChecker;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class ResultExceptionCheckerComposite implements ResultExceptionChecker,ApplicationContextAware {

    Set<ResultExceptionChecker> checkers = new HashSet<ResultExceptionChecker>();

    @Override
    public void check(Object result) throws CommandExecuteException {
         for (ResultExceptionChecker checker:checkers){
             checker.check(result);
         }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      Map<String,ResultExceptionChecker> checkerBeanMap=  applicationContext.getBeansOfType(ResultExceptionChecker.class, false, false);
      if (MapUtils.isNotEmpty(checkerBeanMap)) {
          for (ResultExceptionChecker checker :checkerBeanMap.values()){
              if (!this.equals(checker)){
                  checkers.add(checker);
              }
          }
      }
    }
}
