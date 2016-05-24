package org.nrg.xnat.protocol.entities.subentities;

import org.apache.commons.lang3.builder.CompareToBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;
import org.nrg.xnat.protocol.entities.Protocol;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Auditable
@Entity
public class ExpectedExperiment extends AbstractHibernateEntity implements Comparable<ExpectedExperiment> {
    private Protocol protocol;
    private VisitType visitType;
//    private String createLink;
//    private String deleteLink;
//    private String editLink;
    private String subtype;
    private boolean required = false;
    private boolean acceptMultiple = false;
    private int sortOrder;
    private String type;
    private boolean userEntered = false;
    private List<ExpectedAssessor> expectedAssessors = new ArrayList<>();

    public ExpectedExperiment() {
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

    public ExpectedExperiment(String type, String subtype, boolean required, int sortOrder, boolean userEntered, String createLink, String deleteLink, String editLink) {
//        this.createLink = createLink;
//        this.deleteLink = deleteLink;
//        this.editLink = editLink;
        this.subtype = subtype;
        this.required = required;
        this.sortOrder = sortOrder;
        this.type = type;
        this.userEntered = userEntered;
    }

    @JsonIgnore
    @ManyToOne
    public Protocol getProtocol() {
        return this.protocol;
    }

    @JsonIgnore
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @JsonIgnore
    @ManyToOne
    public VisitType getVisitType() {
        return this.visitType;
    }

    @JsonIgnore
    public void setVisitType(VisitType visitType) {
        this.visitType = visitType;
    }

//    public String getCreateLink() {
//        return this.createLink;
//    }
//
//    public void setCreateLink(String createLink) {
//        this.createLink = createLink;
//    }
//
//    public String getDeleteLink() {
//        return this.deleteLink;
//    }
//
//    public void setDeleteLink(String deleteLink) {
//        this.deleteLink = deleteLink;
//    }
//
//    public String getEditLink() {
//        return this.editLink;
//    }
//
//    public void setEditLink(String editLink) {
//        this.editLink = editLink;
//    }

    public String getSubtype() {
        return this.subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public boolean getRequired() {
        return this.required;
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
        return this.sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getUserEntered() {
        return this.userEntered;
    }

    public void setUserEntered(boolean userEntered) {
        this.userEntered = userEntered;
    }

    @OneToMany(mappedBy = "expectedExperiment", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    public List<ExpectedAssessor> getExpectedAssessors() {
        return this.expectedAssessors;
    }

    public void setExpectedAssessors(List<ExpectedAssessor> expectedAssessors) {
        if(expectedAssessors != null) {
            this.expectedAssessors = expectedAssessors;
            for(ExpectedAssessor ea: expectedAssessors){
                ea.setExpectedExperiment(this);
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(subtype).append("\n");
        sb.append(required).append("\n");
        sb.append(sortOrder).append("\n");
        sb.append(type).append("\n");
        sb.append(userEntered).append("\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpectedExperiment)) return false;
        ExpectedExperiment that = (ExpectedExperiment) o;
        if (acceptMultiple != that.acceptMultiple) return false;
        if (required != that.required) return false;
        if (userEntered != that.userEntered) return false;
        if (expectedAssessors != null && that.expectedAssessors != null) {
            if(expectedAssessors.size() != that.expectedAssessors.size()) return false;
            for (int i = 0; i < expectedAssessors.size(); i++) {
                Object exAs = expectedAssessors.get(i);
                Object expExAs = that.expectedAssessors.get(i);
                if(!exAs.equals(expExAs)) {
                    return false;
                }
            }
        }
        if (sortOrder != that.sortOrder) return false;
        if (subtype != null ? !subtype.equals(that.subtype) : that.subtype != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (subtype != null ? subtype.hashCode() : 0);
        result = 31 * result + (required ? 1 : 0);
        result = 31 * result + (acceptMultiple ? 1 : 0);
        result = 31 * result + sortOrder;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (userEntered ? 1 : 0);
        result = 31 * result + (expectedAssessors != null ? expectedAssessors.hashCode() : 0);
        return result;
    }

    public int compareTo(ExpectedExperiment rhs) {
        return new CompareToBuilder().
                append(this.getSortOrder(), rhs.getSortOrder()).
                toComparison();
    }
}
