package org.nrg.xnat.protocol.daos;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 3/19/14 1:19 PM
 */

import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.protocol.entities.ProtocolLineage;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unchecked")
@Repository
public class ProtocolLineageDAO extends AbstractHibernateDAO<ProtocolLineage> {
}
