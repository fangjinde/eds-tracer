package com.netease.edu.eds.trace.demo.dto;/**
                                              * Created by hzfjd on 18/4/26.
                                              */

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.io.Serializable;

/**
 * @author hzfjd
 * @create 18/4/26
 */
@Document(indexName = "es-demo-index", type = "es-demo-type", useServerConfiguration = true)
public class DemoDocumentDto implements Serializable {

    public DemoDocumentDto() {
    }

    private static final long serialVersionUID = -6385882573362533320L;

    @Id
    private String            id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DemoDocumentDto withName(String name) {
        this.name = name;
        return this;
    }

    public DemoDocumentDto withId(String id) {
        this.id = id;
        return this;
    }

    @Field
    private String name;

}
