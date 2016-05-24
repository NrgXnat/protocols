package org.nrg.xnat.protocol.services.impl.hibernate;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/18/13 1:39 PM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.daos.ProtocolDAO;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.services.ProtocolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class HibernateProtocolService extends AbstractHibernateEntityService<Protocol, ProtocolDAO> implements ProtocolService {
    private static final Log _log = LogFactory.getLog(HibernateProtocolService.class);

    @Override
    @Transactional
    public Protocol getProtocolById(Long protocolId) {
        if (protocolId == null || protocolId == 0) return null;
        Protocol protocol = getDao().findCurrentByProtocolId(protocolId);
        if (protocol != null && !protocol.isEnabled())  protocol = null;
        return protocol;
    }

    @Override
    @Transactional
    public Protocol getProtocolByIdAndVersion(Long protocolId, Integer version) {
        if (protocolId == null || protocolId == 0) return null;
        return getDao().findByProtocolIdandVersion(protocolId, version);
    }

    @Override
    @Transactional
    public Protocol deleteProtocol(Long protocolId) {
        Protocol latest = getProtocolById(protocolId);
        if (latest != null) {
            getDao().delete(latest);
        }
        return latest;
    }

    @Override
    @Transactional
    public List<Protocol> getProtocolHistory(Long protocolId) {
        if (protocolId == null || protocolId == 0) return new ArrayList<Protocol>();
        return getDao().findAllByProtocolId(protocolId);
    }

    @Override
    @Transactional
    public Integer getMaxProtocolVersion(Long protocolId) {
        if (protocolId == null || protocolId == 0) return 0;
        return getDao().findAllByProtocolId(protocolId).size();
    }

    @Override
    @Transactional
    public List<Protocol> getAvailableProtocols(UserI user) {
        List<Protocol> candidateList = getDao().findAvailable();
        List<Protocol> returnList = new ArrayList<Protocol>();
        for (Protocol protocol : candidateList) {
            if (protocol.getUserWhiteList().contains(user.getUsername()) || Roles.isSiteAdmin(user)) {
                returnList.add(protocol);
            }
        }
        Collections.sort(returnList);
        return returnList;
    }

    @Override
    @Transactional
    public Protocol storeProtocol(Protocol protocol, UserI user) {
        protocol.setEnabled(true);
        protocol.setCreatedBy(user.getUsername());
        getDao().create(protocol);
        return protocol;
    }

    @Override
    @Transactional
    public List<ProjectProtocol> getProjectsUsingProtocol(Long protocolId, UserI user) {
        List<ProjectProtocol> results = new ArrayList<ProjectProtocol>();
        List<Protocol> history = getProtocolHistory(protocolId);
        for (Protocol protocol : history) {
            if (protocol.isEnabled())
                for (ProjectProtocol projectProtocol : protocol.getProjectProtocols()) {
                    if (projectProtocol.isEnabled()) {
                        XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectProtocol.getProjectId(), (UserI) user, false);
                        try {
                            if(project.canEdit(user)) {
                                results.add(projectProtocol);
                            }
                        } catch (Exception e) {
                            _log.error("",e);
                        }
                    }
                }
        }
        return results;
    }
}
