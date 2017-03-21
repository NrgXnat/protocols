/*
 * protocols: org.nrg.xnat.protocol.services.impl.hibernate.HibernateProjectProtocolService
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.services.impl.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.daos.ProjectProtocolDAO;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.services.ProjectProtocolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HibernateProjectProtocolService extends AbstractHibernateEntityService<ProjectProtocol, ProjectProtocolDAO> implements ProjectProtocolService {
    private static final Log _log = LogFactory.getLog(HibernateProjectProtocolService.class);

    @Override
    @Transactional
    public Protocol getProtocolForProject(String projectId, UserI user) {
        if (!hasProjectAccess(projectId, user)) return null;
        ProjectProtocol projectProtocol = getDao().findCurrentByProjectId(projectId);
        if (projectProtocol != null && projectProtocol.isEnabled() &&
                projectProtocol.getProtocol().isEnabled()) return projectProtocol.getProtocol();
        return null;
    }

    @Override
    @Transactional
    public Protocol getPreviousProtocolForProject(String projectId, Integer version, UserI user) {
        if (!hasProjectAccess(projectId, user)) return null;
        List<ProjectProtocol> previousProtocols = getDao().findByProjectId(projectId);
        if (previousProtocols != null && version <= previousProtocols.size())
            return previousProtocols.get(version-1).getProtocol();
        return null;
    }

    @Override
    @Transactional
    public void assignProtocolToProject(XnatProjectdata project, Protocol protocol, UserI user) {
        removeProtocolFromProject(project.getId(), user);

        ProjectProtocol projectProtocol = new ProjectProtocol(project.getId(), project.getName(), protocol, user.getUsername());
        create(projectProtocol);
    }

    @Override
    @Transactional
    public void removeProtocolFromProject(String projectId, UserI user) {
//        if (!hasProjectAccess(projectId, user)) return; // Security on this is checked at the REST call. We don't need to here
        // It really should accept an XDATUser object anyway so we can check user.isSiteAdmin() here in addition
        // Also... This should throw a security exception of some sort. Returning silently gives the impression it worked, but it didn't
        // So I'm commenting it out now so we can appropriately delete project protocol associations

        ProjectProtocol projectProtocol = getDao().findCurrentByProjectId(projectId);
        if (projectProtocol != null) delete(projectProtocol);
    }

    private boolean hasProjectAccess(String projectId, UserI user) {
        final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectId, user, false);
        return p != null;
    }
}
