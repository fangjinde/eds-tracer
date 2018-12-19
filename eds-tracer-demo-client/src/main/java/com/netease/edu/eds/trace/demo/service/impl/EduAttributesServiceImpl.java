package com.netease.edu.eds.trace.demo.service.impl;

import com.google.common.collect.Lists;
import com.netease.edu.eds.trace.demo.dao.EduAttributes;
import com.netease.edu.eds.trace.demo.dao.EduAttributesDao;
import com.netease.edu.eds.trace.demo.service.EduAttributesDto;
import com.netease.edu.eds.trace.demo.service.EduAttributesService;
import com.netease.edu.util.common.BeanConvertUtils;
import com.netease.edu.util.sql.SqlBuilder;
import net.spy.memcached.MemcachedClient;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Service("eduAttributesService")
public class EduAttributesServiceImpl implements EduAttributesService {

    private final static Integer CACHE_EXP_TIME       = 30 * 60;
    private final static String  ATTR_VALUE_CACHE_KEY = "S2_ATTRIBUTE_VALUE_";
    private final static String  ATTR_CACHE_KEY       = "S2_ATTRIBUTE_";
    private final        Integer ACTIVE_TRUE          = 1;
    @Autowired EduAttributesDao eduAttributesDao;
    @Autowired
    @Resource(name = "sharedMemcachedClient")
    private MemcachedClient memcachedClient;

    @Override
    public boolean updateAttributes(String key, String value) {
        // TODO Auto-generated method stub
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return false;
        }
        EduAttributes attributes = getAttributes(key);
        if (attributes == null) {
            EduAttributes e = new EduAttributes();
            e.setKey(key);
            e.setP(1);
            e.setValue(value);
            return eduAttributesDao.add(e) > 0;
        }
        EduAttributes update = new EduAttributes();
        update.setId(attributes.getId());
        update.setValue(value);
        boolean success = eduAttributesDao.updateSelectiveById(update) > 0;
        if (success) {
            String cacheKey =  ATTR_VALUE_CACHE_KEY + key;
            memcachedClient.delete(cacheKey);
        }
        return success;
    }

    /**
     * 
     */
    @Override
    public String getAttributesValue(String key) {
        if(StringUtils.isEmpty(key)){
            return null;
        }
        String cacheKey =  ATTR_VALUE_CACHE_KEY + key;
        Object obj = memcachedClient.get(cacheKey);
        if(obj != null){
            return (String)obj;
        }
        EduAttributes e = this.getAttributes(key);
        if (e == null) {
            return null;
        }
        memcachedClient.set(cacheKey, CACHE_EXP_TIME, e.getValue());
        return e.getValue();
    }
    
    private EduAttributes getAttributes(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        String condition = "k = ? and p = ? limit 1";
        List<EduAttributes> eduAttributes = eduAttributesDao.getByCondition(condition, key, ACTIVE_TRUE);
        if (CollectionUtils.isEmpty(eduAttributes)) {
            return null;
        }
        return eduAttributes.get(0);
    }
    
    @Override
    public String getAttributesValueByCacheTime(String key, int limitTime) {
        if(StringUtils.isEmpty(key)){
            return null;
        }
        String cacheKey =  ATTR_VALUE_CACHE_KEY + key;
        Object obj = memcachedClient.get(cacheKey);
        if(obj != null){
            return (String)obj;
        }
        EduAttributes e = this.getAttributes(key);
        if (e == null) {
            return null;
        }
        memcachedClient.set(cacheKey, limitTime, e.getValue());
        return e.getValue();
    }

    @Override
    public boolean addAttributes(String key, String value) {
        EduAttributes e = this.getAttributes(key);
        if (e != null) {
            return true;
        }

        e = new EduAttributes();
        e.setKey(key);
        e.setP(1);
        e.setValue(value);
        return eduAttributesDao.add(e) > 0;
    }

    @Override
    public List<EduAttributesDto> getEduAttributes(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Lists.newArrayList();
        }

        List<EduAttributes> eduAttributes = eduAttributesDao.getByCondition(SqlBuilder.inSql("k", keys) + " and p = ?",
                                                                            ACTIVE_TRUE);
        return BeanConvertUtils.convert(eduAttributes, EduAttributes.class, EduAttributesDto.class);
    }

}
