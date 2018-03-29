package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/29.
 */

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * @author hzfjd
 * @create 18/3/29
 */
public class DdbTraceBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final String DDB_SQL_MANAGER_CLASS_NAME             = "com.netease.edu.persist.dao.sql.SqlManagerEduProxy";
    private static final String DDB_CONNECTION_MANAGER_IMPL_CLASS_NAME = "com.netease.dbsupport.impl.ConnectionManagerDDBImpl";

    private static final String LEGACY_DDB_DATA_SOURCE_CLASS_NAME = "com.netease.edu.persist.dao.utils.NeteaseDataSourceAdapter";

    @Override public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (DDB_SQL_MANAGER_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                beanDefinition.setBeanClassName(SqlManagerTracedImpl.class.getName());
            }

            if (DDB_CONNECTION_MANAGER_IMPL_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                beanDefinition.setBeanClassName(ConnectionManagerDDBTracedImpl.class.getName());
            }

            if (LEGACY_DDB_DATA_SOURCE_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                beanDefinition.setBeanClassName(NeteaseDataSourceAdapterTraced.class.getName());
            }
        }
    }
}
