package com.netease.edu.eds.trace.demo.dto;/**
                                              * Created by hzfjd on 18/4/26.
                                              */

import java.io.Serializable;

/**
 * @author hzfjd
 * @create 18/4/26
 */
public class DemoDto implements Serializable {

    public DemoDto() {
    }

    private static final long serialVersionUID = -6385882573362533320L;

    private Long              id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DemoDto withName(String name) {
        this.name = name;
        return this;
    }

    public DemoDto withId(Long id) {
        this.id = id;
        return this;
    }

    private String name;

}
