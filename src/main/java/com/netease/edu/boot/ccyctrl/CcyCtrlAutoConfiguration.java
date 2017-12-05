package com.netease.edu.boot.ccyctrl;/**
 * Created by hzfjd on 17/12/4.
 */

import com.netease.ccyctrl.component.CcyCtrlConfigurer;
import com.netease.ccyctrl.component.CcyCtrlManager;
import com.netease.ccyctrl.component.CcyCtrlMonitor;
import com.netease.ccyctrl.component.CcyCtrlSupport;
import com.netease.ccyctrl.component.impl.CcyCtrlConfigurerImpl;
import com.netease.ccyctrl.component.impl.CcyCtrlManagerImpl;
import com.netease.ccyctrl.component.impl.CcyCtrlMonitorImpl;
import com.netease.ccyctrl.component.impl.CcyCtrlSupportImpl;
import com.netease.edu.aop.aspect.ConcurrencyCtrlAspectJ;
import com.netease.edu.util.ccyctrl.SpringCloudConfigCcyCtrlConfigManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 并发控制自动配置,see @EnableCcyCtrl
 *
 * @author hzfjd
 * @create 17/12/4
 */
@Configuration
public class CcyCtrlAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CcyCtrlAutoConfiguration.class);

    /**
     * 并发隔离控制aop
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(ConcurrencyCtrlAspectJ.class)
    public ConcurrencyCtrlAspectJ concurrencyCtrlAspectJ() {
        return new ConcurrencyCtrlAspectJ();
    }

    @Bean
    @ConditionalOnMissingBean(CcyCtrlManager.class)
    public CcyCtrlManager ccyCtrlManager() {
        return new CcyCtrlManagerImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CcyCtrlSupport.class)
    public CcyCtrlSupport CcyCtrlSupport() {
        return new CcyCtrlSupportImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CcyCtrlMonitor.class)
    @ConditionalOnClass(CcyCtrlMonitorImpl.class)
    public CcyCtrlMonitor CcyCtrlMonitor() {
        return new CcyCtrlMonitorImpl();
    }


    @Bean
    @ConditionalOnMissingBean(CcyCtrlConfigurer.class)
    public CcyCtrlConfigurer ccyCtrlConfigurer(){
        return new CcyCtrlConfigurerImpl();
    }

    /**
     * 并发隔离阈值配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(SpringCloudConfigCcyCtrlConfigManagement.class)
    public SpringCloudConfigCcyCtrlConfigManagement springCloudConfigCcyCtrlConfigManagement() {
        return new SpringCloudConfigCcyCtrlConfigManagement();
    }

    @Bean
    @ConditionalOnMissingBean(CcyCtrlConfigManagementRefreshListener.class)
    public CcyCtrlConfigManagementRefreshListener ccyCtrlConfigManagementRefreshListener(
            SpringCloudConfigCcyCtrlConfigManagement springCloudConfigCcyCtrlConfigManagement) {
        return new CcyCtrlConfigManagementRefreshListener(springCloudConfigCcyCtrlConfigManagement);
    }

    /**
     * 监听配置刷新事件
     */
    public class CcyCtrlConfigManagementRefreshListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

        private SpringCloudConfigCcyCtrlConfigManagement springCloudConfigCcyCtrlConfigManagement;

        CcyCtrlConfigManagementRefreshListener(SpringCloudConfigCcyCtrlConfigManagement springCloudConfigCcyCtrlConfigManagement){
            this.springCloudConfigCcyCtrlConfigManagement=springCloudConfigCcyCtrlConfigManagement;
        }

        @Override
        public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
            try {
                springCloudConfigCcyCtrlConfigManagement.reload();
            } catch (Exception e) {
                logger.error("reload springCloudConfigCcyCtrlConfigManagement error", e);
            }
        }
    }

}
