package com.netease.edu.eds.shuffle.core;

import org.apache.curator.x.discovery.ServiceInstance;

import java.util.Collection;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public interface ServiceDirectory<T> {

    /**
     * Return the names of all known services
     *
     * @return list of service names
     */
    public Collection<String> queryForNames();

    /**
     * Return all known instances for the given service
     *
     * @param name name of the service
     * @return list of instances (or an empty list)
     */
    public Collection<ServiceInstance<T>> queryForInstances(String name);

    /**
     * Return a service instance POJO
     *
     * @param name name of the service
     * @param id ID of the instance
     * @return the instance or <code>null</code> if not found
     */
    public ServiceInstance<T> queryForInstance(String name, String id);
}
