/*
 * protocols: org.nrg.xnat.protocol.daos.ProjectProtocolDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("unchecked")
@Repository
public class ProjectProtocolDAO extends AbstractHibernateDAO<ProjectProtocol> {

    public List<ProjectProtocol> findByProjectId(String projectId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("projectId", projectId));
        criteria.addOrder(Order.asc("created"));
        return criteria.list();
    }

    public ProjectProtocol findCurrentByProjectId(String projectId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("projectId", projectId));
        criteria.addOrder(Order.desc("created"));
        criteria.setMaxResults(1);
        return (ProjectProtocol)criteria.uniqueResult();
    }
}
