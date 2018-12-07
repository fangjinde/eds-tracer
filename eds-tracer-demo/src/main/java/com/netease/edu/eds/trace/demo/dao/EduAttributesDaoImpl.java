package com.netease.edu.eds.trace.demo.dao;

import org.springframework.stereotype.Component;

import com.netease.edu.persist.dao.annotation.DomainMetadata;
import com.netease.edu.persist.dao.sql.BaseDaoSqlImpl;

@DomainMetadata(domainClass = EduAttributes.class, tableName = "EduAttributes")
@Component("eduAttributesDao")
public class EduAttributesDaoImpl extends BaseDaoSqlImpl<EduAttributes> implements EduAttributesDao {

}
