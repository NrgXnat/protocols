package org.nrg.xnat.protocol.entities;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 2/10/14 11:02 AM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.ArrayList;

@Auditable
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ProjectProtocol extends AbstractHibernateEntity implements Comparable<ProjectProtocol> {
    private static final Log _log = LogFactory.getLog(ProjectProtocol.class);
    private String projectId;
    private String projectName;
    private Protocol protocol;
    private ArrayList<String> properties = new ArrayList<>();
    private String createdBy;

    public ProjectProtocol() {
    }

    public ProjectProtocol(String projectId, String projectName, Protocol protocol, String updateUser) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.protocol = protocol;
        this.createdBy = updateUser;
    }

    @Column(nullable = false)
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String project) {
        this.projectId = project;
    }

    @JsonIgnore
    @ManyToOne
    public Protocol getProtocol() {
        return protocol;
    }

    @JsonIgnore
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Column(nullable = false)
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Column
    public String getProjectName() { return projectName; }

    public void setProjectName(String projectName) { this.projectName = projectName; }

    @Transient
    @JsonProperty("protocolVersion")
    public Integer getProtocolVersion() { return protocol.getVersion(); }

    @Override
    public int compareTo(ProjectProtocol that) {
        return this.projectName.compareToIgnoreCase(that.projectName);
    }

    public ArrayList<String> getProperties() {
        if (this.properties == null) properties = new ArrayList<>();
        return properties;
    }

    public void setProperties(ArrayList<String> properties) {
        this.properties = properties;
    }
}