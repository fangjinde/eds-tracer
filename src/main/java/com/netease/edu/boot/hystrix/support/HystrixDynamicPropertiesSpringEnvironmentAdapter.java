package com.netease.edu.boot.hystrix.support;

import com.netflix.hystrix.strategy.properties.HystrixDynamicProperties;
import com.netflix.hystrix.strategy.properties.HystrixDynamicProperty;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 适配到spring environment
 *
 * @author hzfjd
 * @create 17/12/24
 */
public class HystrixDynamicPropertiesSpringEnvironmentAdapter
        implements HystrixDynamicProperties, EnvironmentAware, ApplicationListener<EnvironmentChangeEvent> {

    public HystrixDynamicPropertiesSpringEnvironmentAdapter() {
    }

    private static Environment s_environment;

    private static Logger logger = LoggerFactory.getLogger(HystrixDynamicPropertiesSpringEnvironmentAdapter.class);

    private static ConcurrentHashMap<String, AbstractHystrixDynamicProperty> props = new ConcurrentHashMap<String, AbstractHystrixDynamicProperty>(
            200);

    private abstract class AbstractHystrixDynamicProperty<T> implements HystrixDynamicProperty<T> {

        List<Runnable> callbacks = new ArrayList<Runnable>(5);
        private String name;

        protected AbstractHystrixDynamicProperty(String name) {
            this.name = name;
        }

        public List<Runnable> getCallbacks() {
            return callbacks;
        }

        @Override
        public synchronized void addCallback(Runnable callback) {
            callbacks.add(callback);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    public HystrixDynamicProperty<String> getString(String name, final String fallback) {

        HystrixDynamicProperty oldHp = props.get(name);

        if (oldHp != null) {
            return oldHp;
        }

        AbstractHystrixDynamicProperty<String> newhp = new AbstractHystrixDynamicProperty<String>(name) {

            @Override
            public String get() {
                String propValue = null;
                if (s_environment != null) {
                    propValue = s_environment.getProperty(getName());
                }
                if (StringUtils.isBlank(propValue)) {
                    propValue = fallback;
                }
                return propValue;
            }
        };

        oldHp = props.putIfAbsent(name, newhp);
        if (oldHp != null) {
            return oldHp;
        } else {
            return newhp;
        }

    }

    @Override
    public HystrixDynamicProperty<Integer> getInteger(String name, final Integer fallback) {

        HystrixDynamicProperty oldHp = props.get(name);

        if (oldHp != null) {
            return oldHp;
        }

        AbstractHystrixDynamicProperty<Integer> newhp = new AbstractHystrixDynamicProperty<Integer>(name) {

            @Override
            public Integer get() {
                String propValue = null;
                if (s_environment != null) {
                    propValue = s_environment.getProperty(getName());
                }

                Integer propIntValue = null;
                if (StringUtils.isNotBlank(propValue)) {
                    try {
                        propIntValue = Integer.parseInt(propValue);
                    } catch (Exception e) {
                        logger.warn("parse property value of ({}) error", getName(), e);
                    }
                }

                if (propIntValue == null) {
                    propIntValue = fallback;
                }

                return propIntValue;
            }
        };

        oldHp = props.putIfAbsent(name, newhp);
        if (oldHp != null) {
            return oldHp;
        } else {
            return newhp;
        }

    }

    @Override
    public HystrixDynamicProperty<Long> getLong(String name, final Long fallback) {

        HystrixDynamicProperty oldHp = props.get(name);

        if (oldHp != null) {
            return oldHp;
        }

        AbstractHystrixDynamicProperty<Long> newhp = new AbstractHystrixDynamicProperty<Long>(name) {

            @Override
            public Long get() {
                String propValue = null;
                if (s_environment != null) {
                    propValue = s_environment.getProperty(getName());
                }

                Long propObjectValue = null;
                if (StringUtils.isNotBlank(propValue)) {
                    try {
                        propObjectValue = Long.parseLong(propValue);
                    } catch (Exception e) {
                        logger.warn("parse property value of ({}) error", getName(), e);
                    }
                }

                if (propObjectValue == null) {
                    propObjectValue = fallback;
                }

                return propObjectValue;
            }
        };

        oldHp = props.putIfAbsent(name, newhp);
        if (oldHp != null) {
            return oldHp;
        } else {
            return newhp;
        }

    }

    @Override
    public HystrixDynamicProperty<Boolean> getBoolean(String name, final Boolean fallback) {

        HystrixDynamicProperty oldHp = props.get(name);

        if (oldHp != null) {
            return oldHp;
        }

        AbstractHystrixDynamicProperty<Boolean> newhp = new AbstractHystrixDynamicProperty<Boolean>(name) {

            @Override
            public Boolean get() {
                String propValue = null;
                if (s_environment != null) {
                    propValue = s_environment.getProperty(getName());
                }

                Boolean propObjectValue = null;
                if (StringUtils.isNotBlank(propValue)) {
                    try {
                        propObjectValue = Boolean.parseBoolean(propValue);
                    } catch (Exception e) {
                        logger.warn("parse property value of ({}) error", getName(), e);
                    }
                }

                if (propObjectValue == null) {
                    propObjectValue = fallback;
                }

                return propObjectValue;
            }
        };

        oldHp = props.putIfAbsent(name, newhp);
        if (oldHp != null) {
            return oldHp;
        } else {
            return newhp;
        }

    }

    @Override
    public void setEnvironment(Environment environment) {

        if (s_environment != null) {
            return;
        }
        synchronized (HystrixDynamicPropertiesSpringEnvironmentAdapter.class) {
            if (s_environment != null) {
                return;
            }
            s_environment = environment;
        }
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {

        for (AbstractHystrixDynamicProperty<?> hp : props.values()) {
            for (Runnable runnable : hp.getCallbacks()) {
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    logger.error("update Dynamic props callback failed.", e);
                }

            }
        }

    }
}
