package com.netease.edu.boot.hystrixtest.service.impl;/**
 * Created by hzfjd on 18/4/19.
 */

import com.netease.edu.boot.hystrixtest.domain.ndir.StudyCourseDocument;
import com.netease.edu.boot.hystrixtest.service.StudyCourseSearchDao;
import com.netease.edu.persist.search.service.AbstractNDirBaseSearchDao;
import org.springframework.stereotype.Component;

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
