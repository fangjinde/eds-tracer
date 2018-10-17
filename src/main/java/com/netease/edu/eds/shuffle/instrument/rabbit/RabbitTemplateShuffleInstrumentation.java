package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.shuffle.support.ShuffleEnvironmentInfoProcessUtils;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.instrument.rabbit.RabbitTemplateInstrumentation;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/9/8
 **/
public class RabbitTemplateShuffleInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.amqp.rabbit.core.RabbitTemplate")).transform((builder,
                                                                                                                           typeDescription,
                                                                                                                           classloader,
                                                                                                                           javaModule) -> builder.method(isOverriddenFrom(typeDescription).and(namedIgnoreCase("doSend")).and(takesArguments(6))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        private static Logger logger = LoggerFactory.getLogger(RabbitTemplateInstrumentation.TraceInterceptor.class);

        private static Object originSend(Object[] args, Invoker invoker) {
            return invoker.invoke(args);
        }

        public static class EnvRabbitInfo {

            public EnvRabbitInfo() {

            }

            public EnvRabbitInfo(String exchange, String env) {
                this.exchange = exchange;
                this.env = env;
            }

            private String exchange;
            private String env;
            private String routingKey;

            public String getExchange() {
                return exchange;
            }

            public void setExchange(String exchange) {
                this.exchange = exchange;
            }

            public String getEnv() {
                return env;
            }

            public void setEnv(String env) {
                this.env = env;
            }

            public String getRoutingKey() {
                return routingKey;
            }

            public void setRoutingKey(String routingKey) {
                this.routingKey = routingKey;
            }

            public EnvRabbitInfo withRoutingKey(String routingKey) {
                this.routingKey = routingKey;
                return this;
            }
        }

        /**
         * 根据RabbitTemplate的实现原理，获取生效的RoutingKey。
         * 
         * @param args
         * @param proxy
         * @return
         */
        private static String getRoutingKeyMergeWithTemplate(Object[] args, Object proxy) {
            String originRoutingKey = (String) args[2];

            if (originRoutingKey == null) {
                RabbitTemplate rabbitTemplate = (RabbitTemplate) proxy;
                originRoutingKey = rabbitTemplate.getRoutingKey();
            }

            return originRoutingKey;
        }

        /**
         * protected void doSend(Channel channel, String exchange, String routingKey, Message message, boolean
         * mandatory, CorrelationData correlationData)
         * 
         * @param args
         * @param invoker
         * @return
         * @throws Exception
         */
        @RuntimeType
        public static Object doSend(@AllArguments Object[] args, @Morph Invoker invoker,
                                    @This Object proxy) throws Exception {

            if (!ShuffleSwitch.isTurnOn()) {
                return originSend(args, invoker);

            }

            if (RabbitShuffleSendContext.shouldIgnoreShuffle()) {
                return originSend(args, invoker);
            }

            String originExchange = (String) args[1];
            String originRoutingKey = getRoutingKeyMergeWithTemplate(args, proxy);

            // 匿名队列的主题，不做双重发送。目前仅仅是springCloudBus需要。
            if (ShufflePropertiesSupport.getAnonymousTopicNames().contains(originExchange)) {
                return originSend(args, invoker);
            }

            List<String> envsForSelection = EnvironmentShuffleUtils.getEnvironmentsForPropagationSelection();
            Map<String, EnvRabbitInfo> allShuffleRabbitInfos = getAllShuffleExchangeToSend(envsForSelection,
                                                                                           originExchange,
                                                                                           originRoutingKey);
            if (MapUtils.isEmpty(allShuffleRabbitInfos)) {
                return originSend(args, invoker);
            }

            Object originResult = null;


            String shffleSendId = UUID.randomUUID().toString().replaceAll("-", "");
            Message message = (Message) args[3];
            message.getMessageProperties().setHeader(ShuffleRabbitConstants.HeaderName.SHUFFLE_SEND_ID_HEADER_NAME,
                                                     shffleSendId);
            int loopIndex = 0;
            for (String env : envsForSelection) {
                EnvRabbitInfo curEnvRabbitInfo = allShuffleRabbitInfos.get(env);
                if (curEnvRabbitInfo == null) {
                    continue;
                }
                args[1] = curEnvRabbitInfo.getExchange();
                args[2] = curEnvRabbitInfo.getRoutingKey();
                if (loopIndex >= 1) {
                    doDelayForNextSend();
                }
                Object result = originSend(args, invoker);
                if (originExchange.equals(curEnvRabbitInfo.getExchange())) {
                    originResult = result;
                }
                loopIndex++;

            }

            return originResult;

        }

        /**
         * wait 150ms to send message to latter environment to reduce cross environment consumer compete as less as
         * possible.
         *
         * @throws InterruptedException
         */
        private static void doDelayForNextSend() throws InterruptedException {
            int delayMs = ShufflePropertiesSupport.getDelayMSToSendLatter();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    logger.error("doDelayForNextSend try to sleep, but got interrupted.", e);
                    throw e;
                }
            }
        }

        private static Map<String, EnvRabbitInfo> getAllShuffleExchangeToSend(List<String> envsForSelection,
                                                                              String originExchange,
                                                                              String originRoutingKey) {

            Map<String, String> allShuffleExchanges = getAllShuffleDestination(envsForSelection, originExchange);

            if (MapUtils.isEmpty(allShuffleExchanges)) {
                return null;
            }

            // exchange有包含环境标识，但RoutingKey可能有，也有可能没有。没有时，返回null。
            Map<String, String> allShuffleRoutingKeys = getAllShuffleDestination(envsForSelection, originRoutingKey);

            Map<String, EnvRabbitInfo> allShuffleRabbitInfo = new LinkedHashMap();

            for (Map.Entry<String, String> entry : allShuffleExchanges.entrySet()) {
                EnvRabbitInfo envRabbitInfo = new EnvRabbitInfo(entry.getValue(), entry.getKey());
                if (allShuffleRoutingKeys != null && allShuffleRoutingKeys.containsKey(entry.getKey())) {
                    envRabbitInfo.withRoutingKey(allShuffleRoutingKeys.get(entry.getKey()));
                } else {
                    // 没有时，返回null，此时应该使用原RoutingKey
                    envRabbitInfo.withRoutingKey(originRoutingKey);
                }

                allShuffleRabbitInfo.put(entry.getKey(), envRabbitInfo);
            }

            return allShuffleRabbitInfo;

        }

        /**
         * 从目标destination的名称中提取对应环境，形成环境-》destination的键值对。然后在补充上要穿梭的环境的键值对。
         * 
         * @param envsForSelection
         * @param originDestination
         * @return
         */
        private static Map<String, String> getAllShuffleDestination(List<String> envsForSelection,
                                                                    String originDestination) {
            if (CollectionUtils.isEmpty(envsForSelection)) {
                return null;
            }

            String originExchangeEnv = null;
            for (String env : envsForSelection) {
                if (originDestination.startsWith(env) || originDestination.endsWith(env)) {
                    originExchangeEnv = env;
                    break;
                }
            }
            // 为规避cloud bus等统一环境exchange的情况，原exchange若无环境属性，则不做shuffle处理
            if (originExchangeEnv == null) {
                return null;
            }

            Map<String, String> allShuffleDestination = new LinkedHashMap();
            allShuffleDestination.put(originExchangeEnv, originDestination);
            // 增加其他环境的exchange，修改对应的环境属性
            for (String newEnv : envsForSelection) {

                if (!originExchangeEnv.equals(newEnv)) {
                    String newShuffleDestination = ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(originDestination,
                                                                                                                      originExchangeEnv,
                                                                                                                      newEnv);
                    if (StringUtils.isNotBlank(newShuffleDestination)) {
                        allShuffleDestination.put(newEnv, newShuffleDestination);
                    }
                }
            }

            return allShuffleDestination;
        }

        public static void main(String[] args) {

            String exchange1 = "memberRegisterExchange-p1";
            String exchange2 = "std-memberRegisterExchange";
            String exchange3 = "memberRegisterExchange";

            String routingKey1 = "routingKey1-p1";
            String routingKey2 = "p1-routingKey2";
            String routingKey3 = "routingKey3";

            System.out.println(TraceJsonUtils.toJson(ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(exchange1,
                                                                                                                        "p1",
                                                                                                                        "std")));
            System.out.println(TraceJsonUtils.toJson(ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(exchange2,
                                                                                                                        "std",
                                                                                                                        "p1")));
            System.out.println(TraceJsonUtils.toJson(ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(exchange3,
                                                                                                                        "p1",
                                                                                                                        "std")));

            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange1,
                                                                                 routingKey1)));
            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange2,
                                                                                 routingKey2)));
            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange3,
                                                                                 routingKey3)));

            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange1,
                                                                                 routingKey1)));
            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange2,
                                                                                 routingKey2)));
            System.out.println(TraceJsonUtils.toJson(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange3,
                                                                                 routingKey3)));

        }

    }
}
