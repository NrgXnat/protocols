package org.nrg.xnat.protocol.daos;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/18/13 1:36 PM
 */

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.protocol.entities.Protocol;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("unchecked")
@Repository
public class ProtocolDAO extends AbstractHibernateDAO<Protocol> {
    public List<Protocol> findAllByProtocolId(Long protocolId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("protocolLineage.id", protocolId));
        criteria.addOrder(Order.asc("version"));
        return criteria.list();
    }

    public Protocol findCurrentByProtocolId(Long protocolId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("protocolLineage.id", protocolId));
        criteria.addOrder(Order.desc("version"));
        criteria.setMaxResults(1);
        return (Protocol)criteria.uniqueResult();
    }

    public Protocol findByProtocolIdandVersion(Long protocolId, Integer version) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("protocolLineage.id", protocolId));
        criteria.add(Restrictions.eq("version", version));
        criteria.setMaxResults(1);
        return (Protocol)criteria.uniqueResult();
    }

    public List<Protocol> findAvailable() {
        /*notes for future self: Hibernate Projections insist on returning your group by type as a column, making it
          impossible just to get a list of ids without resorting to raw sql; for reasons unknown, Hibernate stores its
          primary keys as BigSerials rather than longs*/

        SQLQuery subquery = getSession().createSQLQuery("SELECT MAX(id) AS id FROM xhbm_protocol GROUP BY protocol_lineage").addScalar("id", LongType.INSTANCE);
        List<Long> idList = (List<Long>)subquery.list();
        idList.add(Long.valueOf(0)); // empty lists cause problems for hibernate criteria queries
        Criteria criteria = getCriteriaForType();
        criteria.add(Property.forName("id").in(idList));
        criteria.add(Restrictions.eq("enabled", true));
        criteria.addOrder(Order.asc("name").ignoreCase());
        List<Protocol> results = criteria.list();
        return results;
    }
}
