package com.netease.edu.boot.hystrixtest.service;

import java.io.Serializable;

public class EduAttributesDto implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Long                id;
    private String              key;
    private String              value;
    private Integer             p;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getP() {
        return p;
    }

    public void setP(Integer p) {
        this.p = p;
    }

}
