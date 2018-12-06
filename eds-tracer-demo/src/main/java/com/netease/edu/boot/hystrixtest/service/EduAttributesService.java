package com.netease.edu.boot.hystrixtest.service;

import java.util.List;

public interface EduAttributesService {

    public boolean updateAttributes(String key, String value);

    public String getAttributesValue(String key);
    
    public String getAttributesValueByCacheTime(String key, int limitTime);
    
    public boolean addAttributes(String key, String value);

    List<EduAttributesDto> getEduAttributes(List<String> keys);
}
