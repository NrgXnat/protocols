package org.nrg.xnat.protocol.services.impl.hibernate;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 3/19/14 1:21 PM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnat.protocol.daos.ProtocolLineageDAO;
import org.nrg.xnat.protocol.entities.ProtocolLineage;
import org.nrg.xnat.protocol.services.ProtocolLineageService;
import org.springframework.stereotype.Service;

@Service
public class HibernateProtocolLineageService extends AbstractHibernateEntityService<ProtocolLineage, ProtocolLineageDAO> implements ProtocolLineageService {
    private static final Log _log = LogFactory.getLog(HibernateProtocolLineageService.class);
}
