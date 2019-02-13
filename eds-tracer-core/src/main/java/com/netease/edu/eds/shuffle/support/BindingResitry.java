package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.trace.utils.ConcurrentHashSet;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Iterator;
import java.util.Set;

/**
 * @author hzfjd
 * @create 18/9/8
 **/
public class BindingResitry {

    private static Set<BindingHolder> bindingHolders = new ConcurrentHashSet<>();

    public static void add(BindingHolder bindingHolder) {
        bindingHolders.add(bindingHolder);
    }

    public static boolean contain(BindingHolder bindingHolder) {
        return bindingHolders.contains(bindingHolder);
    }

    public static Iterator<BindingHolder> getIterator() {
        return bindingHolders.iterator();
    }

    public static class BindingHolder {

        private Binding     binding;
        private RabbitAdmin rabbitAdmin;

        public BindingHolder(Binding binding, RabbitAdmin rabbitAdmin) {
            this.binding = binding;
            this.rabbitAdmin = rabbitAdmin;
        }

        public Binding getBinding() {
            return binding;
        }

        public void setBinding(Binding binding) {
            this.binding = binding;
        }

        public RabbitAdmin getRabbitAdmin() {
            return rabbitAdmin;
        }

        public void setRabbitAdmin(RabbitAdmin rabbitAdmin) {
            this.rabbitAdmin = rabbitAdmin;
        }

        @Override
        public int hashCode() {
            int hashCode = 1;
            hashCode = 31 * hashCode + binding.hashCode();
            hashCode = 31 * hashCode + rabbitAdmin.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BindingHolder)) {
                return false;
            }

            BindingHolder other = (BindingHolder) obj;

            return binding.equals(other.getBinding()) && rabbitAdmin.equals(other.getRabbitAdmin());

        }
    }
}
