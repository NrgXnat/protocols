/*
 * protocols: org.nrg.xnat.protocol.entities.subentities.ExpectedAssessor
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.entities.subentities;
 

import org.apache.commons.lang3.builder.CompareToBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;
import org.nrg.xnat.protocol.entities.Protocol;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Date;

@Auditable
@Entity
public class ExpectedAssessor extends AbstractHibernateEntity implements Comparable<ExpectedAssessor> {
    private ExpectedExperiment expectedExperiment;
    private boolean required = false;
    private boolean acceptMultiple = false;
    private int sortOrder;
    private String type;
    private String subtype;

    public ExpectedAssessor() {
        Date now = new Date();
        this.setEnabled(true);
        if(this.getCreated() == null){
            this.setCreated(now);
        }
        if(this.getDisabled() == null){
            this.setDisabled(now);
        }
        if(this.getTimestamp() == null){
            this.setTimestamp(now);
        }
    }

    @JsonIgnore
    @ManyToOne
    public ExpectedExperiment getExpectedExperiment() {
        return expectedExperiment;
    }

    @JsonIgnore
    public void setExpectedExperiment(ExpectedExperiment ee) { this.expectedExperiment = ee; }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean getAcceptMultiple() {
        return acceptMultiple;
    }

    public void setAcceptMultiple(boolean acceptMultiple) {
        this.acceptMultiple = acceptMultiple;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpectedAssessor)) return false;
        ExpectedAssessor that = (ExpectedAssessor) o;
        if (acceptMultiple != that.acceptMultiple) return false;
        if (required != that.required) return false;
        if (sortOrder != that.sortOrder) return false;
        if (subtype != null ? !subtype.equals(that.subtype) : that.subtype != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (required ? 1 : 0);
        result = 31 * result + (acceptMultiple ? 1 : 0);
        result = 31 * result + sortOrder;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (subtype != null ? subtype.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(ExpectedAssessor rhs) {
        return new CompareToBuilder().
                append(this.getSortOrder(), rhs.getSortOrder()).
                toComparison();
    }
}
