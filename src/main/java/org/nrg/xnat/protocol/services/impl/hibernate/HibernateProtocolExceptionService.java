package org.nrg.xnat.protocol.services.impl.hibernate;/*
 * org.nrg.xnat.protocol.services.impl.hibernate.HibernateExceptionService
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/27/13 11:11 AM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnat.protocol.daos.ProtocolExceptionDAO;
import org.nrg.xnat.protocol.entities.ProtocolException;
import org.nrg.xnat.protocol.services.ProtocolExceptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HibernateProtocolExceptionService extends AbstractHibernateEntityService<ProtocolException, ProtocolExceptionDAO> implements ProtocolExceptionService {

    @Override
    @Transactional
    public List<ProtocolException> findExceptionsForSubject(String subjectId, List<String> projectIds) {
        return getDao().findBySubjectId(subjectId, projectIds);
    }

    @Override
    @Transactional
    public List<ProtocolException> findExceptionsForVisit(String visitId) {
        return getDao().findByVisitId(visitId);
    }

    @Override
    @Transactional
    public ProtocolException findExceptionForVisitAndType(String visitId, String xsiType, String subtype) {
        return getDao().findByVisitAndType(visitId, xsiType, subtype);
    }

    private static final Log _log = LogFactory.getLog(HibernateProtocolExceptionService.class);
}
