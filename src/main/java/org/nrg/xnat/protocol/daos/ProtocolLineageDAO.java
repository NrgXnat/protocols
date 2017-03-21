/*
 * protocols: org.nrg.xnat.protocol.daos.ProtocolLineageDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.daos;

import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.protocol.entities.ProtocolLineage;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unchecked")
@Repository
public class ProtocolLineageDAO extends AbstractHibernateDAO<ProtocolLineage> {
}
