package org.nrg.xnat.protocol.services;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/18/13 1:38 PM
 */

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;

import java.util.List;

public interface ProtocolService extends BaseHibernateService<Protocol> {
    abstract public Protocol getProtocolById(Long protocolId);
    abstract public Protocol getProtocolByIdAndVersion(Long protocolId, Integer version);
    abstract public Protocol deleteProtocol(Long protocolId);
    abstract public List<Protocol> getProtocolHistory(Long protocolId);
    abstract public Integer getMaxProtocolVersion(Long protocolId);
    abstract public List<Protocol> getAvailableProtocols(UserI user);
    abstract public Protocol storeProtocol(Protocol protocol, UserI user);
    abstract public List<ProjectProtocol> getProjectsUsingProtocol(Long protocolId, UserI user);
}
