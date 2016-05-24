package org.nrg.xnat.protocol.entities;/*
 * org.nrg.xnat.protocol.entities.ProtocolException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/27/13 10:46 AM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.sql.Date;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ProtocolException extends AbstractHibernateEntity {

    public ProtocolException() {
    }

    public ProtocolException(String _reason, String _projectId, String _subjectId, String _visitId, String _xsiType, String _authorizingUser) {
        this._reason = _reason;
        this._projectId = _projectId;
        this._visitId = _visitId;
        this._subjectId = _subjectId;
        this._xsiType = _xsiType;
        this._authorizingUser = _authorizingUser;
    }

    @Column(nullable = false)
    public String get_authorizingUser() {
        return _authorizingUser;
    }

    public void set_authorizingUser(String _authorizingUser) {
        this._authorizingUser = _authorizingUser;
    }

    public Date get_date() {
        return _date;
    }

    public void set_date(Date _date) {
        this._date = _date;
    }

    public String get_reason() {
        return _reason;
    }

    public void set_reason(String _reason) {
        this._reason = _reason;
    }

    public String get_explanation() {
        return _explanation;
    }

    public void set_explanation(String _explanation) {
        this._explanation = _explanation;
    }

    @Column(nullable = false)
    public String get_xsiType() {
        return _xsiType;
    }

    public void set_xsiType(String _xsiType) {
        this._xsiType = _xsiType;
    }

    public String get_subtype() {
        return _subtype;
    }

    public void set_subtype(String _subtype) {
        this._subtype = _subtype;
    }

    @Column(nullable = false)
    public String get_visitId() {
        return _visitId;
    }

    public void set_visitId(String _visitId) {
        this._visitId = _visitId;
    }

    @Column(nullable = false)
    public String get_subjectId() {
        return _subjectId;
    }

    public void set_subjectId(String _subjectId) {
        this._subjectId = _subjectId;
    }

    @Column(nullable = false)
    public String get_projectId() {
        return _projectId;
    }

    public void set_projectId(String _projectId) {
        this._projectId = _projectId;
    }

    private static final Log _log = LogFactory.getLog(ProtocolException.class);
    private String _authorizingUser;
    private Date _date;
    private String _reason;
    private String _explanation;
    private String _xsiType;
    private String _subtype;
    private String _visitId;
    private String _subjectId;
    private String _projectId;

}
