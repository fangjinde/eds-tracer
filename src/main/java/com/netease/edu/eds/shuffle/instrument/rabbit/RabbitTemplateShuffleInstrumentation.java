package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.instrument.rabbit.RabbitTemplateInstrumentation;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

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

        @RuntimeType
        public static Object doSend(@AllArguments Object[] args, @Morph Invoker invoker) throws Exception {

            if (!ShuffleSwitch.isTurnOn()) {
                return originSend(args, invoker);

            }

            if (RabbitShuffleSendContext.shouldIgnoreShuffle()) {
                return originSend(args, invoker);
            }

            String exchange = (String) args[1];

            // 匿名队列的主题，不做双重发送。目前仅仅是springCloudBus需要。
            if (ShufflePropertiesSupport.getAnonymousTopicNames().contains(exchange)) {
                return originSend(args, invoker);
            }

            List<String> envsForSelection = EnvironmentShuffleUtils.getEnvironmentsForPropagationSelection();
            List<String> allShuffleExchanges = getAllShuffleExchangeToSend(envsForSelection, exchange);
            if (CollectionUtils.isEmpty(allShuffleExchanges)) {
                return originSend(args, invoker);
            }

            Object originResult = null;

            int loopIndex = 0;
            String shffleSendId = UUID.randomUUID().toString().replaceAll("-", "");
            Message message = (Message) args[3];
            message.getMessageProperties().setHeader(ShuffleRabbitConstants.HeaderName.SHUFFLE_SEND_ID_HEADER_NAME,
                                                     shffleSendId);

            for (String shuffleExchange : allShuffleExchanges) {
                args[1] = shuffleExchange;
                if (loopIndex >= 1) {
                    doDelayForNextSend();
                }
                Object result = originSend(args, invoker);
                if (exchange.equals(shuffleExchange)) {
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

        private static List<String> getAllShuffleExchangeToSend(List<String> envsForSelection, String exchange) {

            if (CollectionUtils.isEmpty(envsForSelection)) {
                return null;
            }

            String originExchangeEnv = null;
            for (String env : envsForSelection) {
                if (exchange.startsWith(env) || exchange.endsWith(env)) {
                    originExchangeEnv = env;
                    break;
                }
            }
            // 为规避cloud bus等统一环境exchange的情况，原exchange若无环境属性，则不做shuffle处理
            if (originExchangeEnv == null) {
                return null;
            }

            List<String> allShuffleExchanges = new ArrayList<>();
            allShuffleExchanges.add(exchange);
            // 增加其他环境的exchange，修改对应的环境属性
            for (String newEnv : envsForSelection) {

                if (!originExchangeEnv.equals(newEnv)) {
                    String newShuffleExchange = getNewExchangeWithNewEnv(exchange, originExchangeEnv, newEnv);
                    if (StringUtils.isNotBlank(newShuffleExchange)) {
                        allShuffleExchanges.add(newShuffleExchange);
                    }
                }
            }

            return allShuffleExchanges;

        }

        public static void main(String[] args) {

            String exchange1 = "memberRegisterExchange-p1";
            String exchange2 = "std-memberRegisterExchange";
            String exchange3 = "memberRegisterExchange";

            System.out.println(getNewExchangeWithNewEnv(exchange1, "p1", "std"));
            System.out.println(getNewExchangeWithNewEnv(exchange2, "std", "p1"));
            System.out.println(getNewExchangeWithNewEnv(exchange3, "p1", "std"));

            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange1));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange2));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange3));

            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange1));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange2));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange3));

        }

        private static String getNewExchangeWithNewEnv(String exchange, String originEnv, String newEnv) {
            StringBuilder newExchangeSb = new StringBuilder();
            if (exchange.startsWith(originEnv)) {
                newExchangeSb.append(newEnv).append(exchange.substring(originEnv.length()));
            } else if (exchange.endsWith(originEnv)) {
                newExchangeSb.append(exchange.substring(0, exchange.length() - originEnv.length())).append(newEnv);
            }
            return newExchangeSb.toString();
        }

    }
}
