/*
 * protocols: org.nrg.xnat.protocol.entities.ProtocolLineage
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ProtocolLineage extends AbstractHibernateEntity {
    private List<Protocol> protocols;
    private ArrayList<String> userWhiteList = new ArrayList<String>();

    public ProtocolLineage() {
        Date now = new Date();
        setCreated(now);
        setTimestamp(now);
        setEnabled(true);
        setDisabled(new Date(0));
    }

    @OneToMany(mappedBy = "protocolLineage", fetch = FetchType.EAGER)
    public List<Protocol> getProtocols() { return protocols; }

    public void setProtocols(List<Protocol> protocols) {
        this.protocols = protocols;
    }

    public ArrayList<String> getUserWhiteList() {
        return userWhiteList;
    }

    public void setUserWhiteList(ArrayList<String> userWhiteList) {
        this.userWhiteList = userWhiteList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProtocolLineage)) return false;

        ProtocolLineage that = (ProtocolLineage) o;

        if (userWhiteList != null ? !userWhiteList.equals(that.userWhiteList) : that.userWhiteList != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return userWhiteList != null ? userWhiteList.hashCode() : 0;
    }
}
