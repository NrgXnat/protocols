package org.nrg.xnat.protocol.daos;/*
 * org.nrg.xnat.protocol.daos.ExceptionDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/27/13 11:00 AM
 */

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.protocol.entities.ProtocolException;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("unchecked")
@Repository
public class ProtocolExceptionDAO extends AbstractHibernateDAO<ProtocolException> {

    public List<ProtocolException> findBySubjectId(String subjectId, List<String> projectIds) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("_subjectId", subjectId));
        criteria.add(Restrictions.in("_projectId", projectIds));
        return criteria.list();
    }

    public List<ProtocolException> findByVisitId(String visitId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("_visitId", visitId));
        return criteria.list();
    }

    public ProtocolException findByVisitAndType(String visitId, String xsiType, String subtype) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("_visitId", visitId));
        criteria.add(Restrictions.eq("_xsiType", xsiType));
        if (StringUtils.isEmpty(subtype)) {
            criteria.add(Restrictions.isNull("_subtype"));
        }
        else {
            criteria.add(Restrictions.eq("_subtype", subtype));
        }

         List<ProtocolException> results = criteria.list();
        if (results.size() == 0 || results.size() > 1) return null; // if we can't identify a single specific deviation, return null
        else return results.get(0);
    }
}
