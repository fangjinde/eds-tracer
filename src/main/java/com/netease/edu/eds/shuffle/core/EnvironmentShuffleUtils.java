package com.netease.edu.eds.shuffle.core;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class EnvironmentShuffleUtils {

    /**
     * 获取当前环境
     * 
     * @return
     */
    public static String getCurrentEnv() {
        Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
        if (environment == null) {
            return null;
        }
        return environment.getProperty("spring.profiles.active");
    }

    /**
     * 获取当前应用名字
     * 
     * @return
     */
    public static String getCurrentApplicationName() {
        Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
        if (environment == null) {
            return null;
        }
        return environment.getProperty("spring.application.name");
    }

    /**
     * 获取用于传播的环境名称,按照优先序返回列表。传播过来的环境 > 当前环境 > 基准环境
     * 
     * @return
     */
    public static List<String> getEnvironmentsForPropagationSelection() {

        return getAllEnvironmentsForPropagationSelection(true);

    }

    private static List<String> getAllEnvironmentsForPropagationSelection(boolean includeCurrentEnv) {
        List<String> environmentsForPropagationSelection = new ArrayList<>(3);
        String lastEnv = null;

        String originEnv = PropagationUtils.getOriginEnv();
        if (StringUtils.isNotBlank(originEnv)) {
            environmentsForPropagationSelection.add(originEnv);
            lastEnv = originEnv;
        }

        if (includeCurrentEnv) {
            String currentEnv = getCurrentEnv();
            if (StringUtils.isNotBlank(currentEnv) && !currentEnv.equals(lastEnv)) {
                environmentsForPropagationSelection.add(currentEnv);
                lastEnv = currentEnv;
            }
        }

        String stdEnv = ShufflePropertiesSupport.getStandardEnvName();
        if (StringUtils.isNotBlank(stdEnv) && !stdEnv.equals(lastEnv)) {
            environmentsForPropagationSelection.add(stdEnv);
        }
        return environmentsForPropagationSelection;
    }

    /**
     * 获取用于传播的环境名称,按照优先序返回列表。传播过来的环境 > 基准环境
     * 
     * @return
     */
    public static List<String> getOriginAndStdEnvironments() {
        return getAllEnvironmentsForPropagationSelection(false);
    }

}
