/*
 * protocols: org.nrg.xnat.protocol.services.ProjectProtocolService
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.services;
 

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;

public interface ProjectProtocolService extends BaseHibernateService<ProjectProtocol> {
    abstract public Protocol getProtocolForProject(String projectId, UserI user);
    abstract public Protocol getPreviousProtocolForProject(String projectId, Integer version, UserI user);
    abstract public void assignProtocolToProject(XnatProjectdata project, Protocol protocol, UserI user);
    abstract public void removeProtocolFromProject(String projectId, UserI user);
}
