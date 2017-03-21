/*
 * protocols: org.nrg.xnat.protocol.services.ProtocolExceptionService
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnat.protocol.entities.ProtocolException;

import java.util.List;

public interface ProtocolExceptionService extends BaseHibernateService<ProtocolException> {

    abstract public List<ProtocolException> findExceptionsForSubject(String subjectId, List<String> projectIds);

    abstract public List<ProtocolException> findExceptionsForVisit(String visitId);

    abstract public ProtocolException findExceptionForVisitAndType(String visitId, String xsiType, String subtype);
}
