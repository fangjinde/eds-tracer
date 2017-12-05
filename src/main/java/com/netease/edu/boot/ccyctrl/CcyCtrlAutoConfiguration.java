package com.netease.edu.boot.ccyctrl;/**
 * Created by hzfjd on 17/12/4.
 */

import com.netease.ccyctrl.component.CcyCtrlManager;
import com.netease.ccyctrl.component.CcyCtrlMonitor;
import com.netease.ccyctrl.component.CcyCtrlSupport;
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
 * 临时复制,后面做成单独的auto-configure
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
    @ConditionalOnMissingBean
    public ConcurrencyCtrlAspectJ concurrencyCtrlAspectJ() {
        return new ConcurrencyCtrlAspectJ();
    }

    @Bean
    @ConditionalOnMissingBean
    public CcyCtrlManager ccyCtrlManager() {
        return new CcyCtrlManagerImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public CcyCtrlSupport CcyCtrlSupport() {
        return new CcyCtrlSupportImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(CcyCtrlMonitorImpl.class)
    public CcyCtrlMonitor CcyCtrlMonitor() {
        return new CcyCtrlMonitorImpl();
    }

    /**
     * 并发隔离阈值配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringCloudConfigCcyCtrlConfigManagement springCloudConfigCcyCtrlConfigManagement() {
        return new SpringCloudConfigCcyCtrlConfigManagement();
    }

    @Bean
    @ConditionalOnMissingBean
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
