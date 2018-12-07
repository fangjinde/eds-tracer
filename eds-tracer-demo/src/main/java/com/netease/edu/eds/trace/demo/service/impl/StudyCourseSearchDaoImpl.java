package com.netease.edu.eds.trace.demo.service.impl;/**
 * Created by hzfjd on 18/4/19.
 */

import org.springframework.stereotype.Component;

import com.netease.edu.eds.trace.demo.domain.ndir.StudyCourseDocument;
import com.netease.edu.eds.trace.demo.service.StudyCourseSearchDao;
import com.netease.edu.persist.search.service.AbstractNDirBaseSearchDao;

/**
 * @author hzfjd
 * @create 18/4/19
 */
@Component
public class StudyCourseSearchDaoImpl extends AbstractNDirBaseSearchDao<StudyCourseDocument> implements StudyCourseSearchDao {

    @Override public Class<?> getDocumentClazz() {
        return StudyCourseDocument.class;
    }
}
