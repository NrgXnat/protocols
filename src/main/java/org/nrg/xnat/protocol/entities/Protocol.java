package org.nrg.xnat.protocol.entities;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;
import org.nrg.xnat.protocol.entities.subentities.ExpectedAssessor;
import org.nrg.xnat.protocol.entities.subentities.ExpectedExperiment;
import org.nrg.xnat.protocol.entities.subentities.VisitType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Auditable
@Entity
@JsonIgnoreProperties({"projectProtocols","protocolLineage"})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class Protocol extends AbstractHibernateEntity implements Comparable<Protocol> {
    private ProtocolLineage protocolLineage = new ProtocolLineage();
    private String name;
    private String description;
    private String armOrder;
    private List<Protocol> arms = new ArrayList<>();
    private Protocol parentArm;
    private String versionDescription; // used to determine protocol equivalency for sharing purposes
    private Integer version; // used to track version of protocol
    private List<VisitType> visitTypes = new ArrayList<>();
    private boolean allowUnexpectedExperiments = false;
    private boolean allowUnexpectedAdHocVisits = false;
    private boolean unexpectedExperimentsRequireApproval = true;
    private boolean allowMultipleOpenVisits = false;
    private boolean allowExceptions = false;
    private boolean enableNonVisitDataCollection = false;
    private ArrayList<String> allowedExceptions = new ArrayList<>();
    private ArrayList<String> headerNotifications = new ArrayList<>();
    private ArrayList<String> emailNotifications = new ArrayList<>();
    private ArrayList<String> defaultNotificationEmails = new ArrayList<>();
//    private List<ExpectedExperiment> ongoingExperiments = new ArrayList<>();
    private String createdBy;
    private List<ProjectProtocol> projectProtocols = new ArrayList<>();

    @ManyToOne
    public ProtocolLineage getProtocolLineage() {
        return protocolLineage;
    }

    public void setProtocolLineage(ProtocolLineage protocolLineage) {
        this.protocolLineage = protocolLineage;
    }

    @JsonProperty("protocolId")
    @Transient
    public Long getProtocolId() {
        return protocolLineage.getId();
    }

    @JsonProperty("protocolId")
    @Transient
    public void setProtocolId(Long protocolId) {
        this.protocolLineage.setId(protocolId);
    }

    @JsonProperty("userWhiteList")
    @Transient
    public ArrayList<String> getUserWhiteList() {
        return protocolLineage.getUserWhiteList();
    }

    @JsonProperty("userWhiteList")
    @Transient
    public void setUserWhiteList(ArrayList<String> userWhiteList) {
        this.protocolLineage.setUserWhiteList(userWhiteList);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArmOrder() {
        return armOrder;
    }

    public void setArmOrder(String armOrder) {
        this.armOrder = armOrder;
    }

    @OneToMany(mappedBy = "parentArm", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    public List<Protocol> getArms() {
        return this.arms;
    }

    public void setArms(List<Protocol> arms) {
        if(arms != null) {
            this.arms = arms;
            for(Protocol arm: this.arms){
                arm.setParentArm(this);
            }
        }
    }

    @JsonIgnore
    @ManyToOne
    public Protocol getParentArm(){ return this.parentArm; }

    @JsonIgnore
    public void setParentArm(Protocol arm){ this.parentArm = arm; }

    public String getVersionDescription() {
        return this.versionDescription;
    }

    public void setVersionDescription(String versionDescription) {
        this.versionDescription = versionDescription;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @OneToMany(mappedBy = "protocol", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @OrderBy("sortOrder ASC")
    public List<VisitType> getVisitTypes() {
        return this.visitTypes;
    }

    public void setVisitTypes(List<VisitType> visitTypes) {
        if(visitTypes != null) {
            this.visitTypes = visitTypes;
            for(VisitType vt: visitTypes){
                vt.setProtocol(this);
            }
        }
    }

    public boolean getAllowUnexpectedExperiments() {
        return allowUnexpectedExperiments;
    }

    public void setAllowUnexpectedExperiments(boolean allowUnexpectedExperiments) {
        this.allowUnexpectedExperiments = allowUnexpectedExperiments;
    }

    public boolean getAllowUnexpectedAdHocVisits() {
        return allowUnexpectedAdHocVisits;
    }

    public void setAllowUnexpectedAdHocVisits(boolean allowUnexpectedAdHocVisits) {
        this.allowUnexpectedAdHocVisits = allowUnexpectedAdHocVisits;
    }

    public boolean getUnexpectedExperimentsRequireApproval() {
        return unexpectedExperimentsRequireApproval;
    }

    public void setUnexpectedExperimentsRequireApproval(boolean unexpectedExperimentsRequireApproval) {
        this.unexpectedExperimentsRequireApproval = unexpectedExperimentsRequireApproval;
    }

    public boolean getAllowMultipleOpenVisits() {
        return allowMultipleOpenVisits;
    }

    public void setAllowMultipleOpenVisits(boolean allowMultipleOpenVisits) {
        this.allowMultipleOpenVisits = allowMultipleOpenVisits;
    }

    public boolean getAllowExceptions() {
        return allowExceptions;
    }

    public void setAllowExceptions(boolean allowExceptions) {
        this.allowExceptions = allowExceptions;
    }

    public boolean getEnableNonVisitDataCollection() {
        return enableNonVisitDataCollection;
    }

    public void setEnableNonVisitDataCollection(boolean enableNonVisitDataCollection) {
        this.enableNonVisitDataCollection = enableNonVisitDataCollection;
    }

    public ArrayList<String> getAllowedExceptions() {
        if (this.allowedExceptions == null) allowedExceptions = new ArrayList<>();
        return allowedExceptions;
    }

    public void setAllowedExceptions(ArrayList<String> allowedExceptions) {
        this.allowedExceptions = allowedExceptions;
    }

    public ArrayList<String> getHeaderNotifications() {
        if (this.headerNotifications == null) headerNotifications = new ArrayList<>();
        return headerNotifications;
    }

    public void setHeaderNotifications(ArrayList<String> headerNotifications) {
        this.headerNotifications = headerNotifications;
    }

    public ArrayList<String> getEmailNotifications() {
        if (this.emailNotifications == null) emailNotifications = new ArrayList<>();
        return emailNotifications;
    }

    public void setEmailNotifications(ArrayList<String> emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public ArrayList<String> getDefaultNotificationEmails() {
        if (this.defaultNotificationEmails == null) defaultNotificationEmails = new ArrayList<>();
        return defaultNotificationEmails;
    }

    public void setDefaultNotificationEmails(ArrayList<String> defaultNotificationEmails) {
        this.defaultNotificationEmails = defaultNotificationEmails;
    }
/*
    @OneToMany(mappedBy = "protocol", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    public List<ExpectedExperiment> getOngoingExperiments() { return this.ongoingExperiments; }

    public void setOngoingExperiments(List<ExpectedExperiment> ongoingExperiments) {
        if(ongoingExperiments != null) {
            this.ongoingExperiments = ongoingExperiments;
            for(ExpectedExperiment ee: ongoingExperiments){
                ee.setProtocol(this);
            }
        }
    }
*/
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<ExpectedExperiment> getExpectedExperiments(String visitTypeName) {
        List<VisitType> types = this.getVisitTypes();
        for (VisitType type : types) {
            if (StringUtils.equalsIgnoreCase(type.getName(), visitTypeName)) {
                return type.getExpectedExperiments();
            }
        }
        return new ArrayList<ExpectedExperiment>();
    }

    @OneToMany(mappedBy = "protocol")
    public List<ProjectProtocol> getProjectProtocols() {
        return projectProtocols;
    }

    public void setProjectProtocols(List<ProjectProtocol> projectProtocols) {
        this.projectProtocols = projectProtocols;
    }

    public boolean isExpectedExperiment(String visitTypeName, String experimentType) {
        List<ExpectedExperiment> exps = this.getExpectedExperiments(visitTypeName);
        for (ExpectedExperiment ex : exps) {
            if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(ex.getType(), experimentType)) {
                return true;
            }
        }
        return false;
    }

    //returns null if the experimenttype passed in is not expected for this visitType. otherwise, it returns the expected experiment so that you can get moar datas abouts it.
    public ExpectedExperiment getExpectedExperiment(String visitTypeName, String experimentType) {
        List<ExpectedExperiment> exps = this.getExpectedExperiments(visitTypeName);
        for (ExpectedExperiment ex : exps) {
            if (StringUtils.equalsIgnoreCase(ex.getType(), experimentType)) {
                return ex;
            }
        }
        return null;
    }

    public VisitType getVisitType(String visitTypeName) {
        for (VisitType vt : visitTypes) {
            if (vt.getName().equals(visitTypeName)) return vt;
        }
        return null;
    }

    //method checks the protocol for any errors. Specifically, having a visitName with a validType that isn't specified in visitTypes.
    //we can always add more as the need arises. For ex, should we check types to make sure they're valid? we could, no problem.
    //returns null if valid. returns a string of error messages if invalid.
    public String validate() {

        StringBuffer errors = new StringBuffer();

        for (VisitType vt : visitTypes) {
            errors.append(vt.validate(visitTypes));
        }

        //TODO: better error handling or logging goes here.
        if (errors.length() < 1) {
            return null;
        } else {
            return errors.toString();
        }
    }

    public String toHTML() {
        StringBuffer sb = new StringBuffer();

        sb.append("<li>").append(versionDescription).append("</li>");

        sb.append("<li>VisitTypes<ul>");
        for (VisitType vt : visitTypes) {
            sb.append("<li>").append(vt.getName()).append("</li>");
            sb.append("<li>").append(vt.getDescription()).append("</li>");
            sb.append("<li>Expected Experiments<ul>");
            for (ExpectedExperiment ee : vt.getExpectedExperiments()) {
                sb.append("<li>").append(ee.getType()).append("</li>");
                sb.append("<li>").append(ee.getSubtype()).append("</li>");
                sb.append("<li>").append(ee.getRequired()).append("</li>");
                sb.append("<li>").append(ee.getSortOrder()).append("</li>");
                sb.append("<li>").append(ee.getUserEntered()).append("</li>");
            }
            sb.append("</ul></li>");
        }
        sb.append("</ul></li>");

        sb.append("</ul>");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Protocol)) return false;
        Protocol protocol = (Protocol) o;
        if (allowExceptions != protocol.allowExceptions) return false;
        if (allowUnexpectedAdHocVisits != protocol.allowUnexpectedAdHocVisits) return false;
        if (enableNonVisitDataCollection != protocol.enableNonVisitDataCollection) return false;
        if (allowMultipleOpenVisits != protocol.allowMultipleOpenVisits) return false;
        if (allowUnexpectedExperiments != protocol.allowUnexpectedExperiments) return false;
        if (unexpectedExperimentsRequireApproval != protocol.unexpectedExperimentsRequireApproval) return false;
        if (allowedExceptions != null ? !allowedExceptions.equals(protocol.allowedExceptions) : protocol.allowedExceptions != null) return false;
        if (headerNotifications != null ? !headerNotifications.equals(protocol.headerNotifications) : protocol.headerNotifications != null) return false;
        if (emailNotifications != null ? !emailNotifications.equals(protocol.emailNotifications) : protocol.emailNotifications != null) return false;
        if (defaultNotificationEmails != null ? !defaultNotificationEmails.equals(protocol.defaultNotificationEmails) : protocol.defaultNotificationEmails != null) return false;
        if (description != null ? !description.equals(protocol.description) : protocol.description != null) return false;
        if (name != null ? !name.equals(protocol.name) : protocol.name != null) return false;
//        if (ongoingExperiments != null ? !ongoingExperiments.equals(protocol.ongoingExperiments) : protocol.ongoingExperiments != null) return false;
        if (version != null ? !version.equals(protocol.version) : protocol.version != null) return false;
        if (versionDescription != null ? !versionDescription.equals(protocol.versionDescription) : protocol.versionDescription != null) return false;
        if (armOrder != null ? !armOrder.equals(protocol.armOrder) : protocol.armOrder != null) return false;
        if (arms != null && protocol.arms != null) {
            if(arms.size() != protocol.arms.size()) return false;
            for (int i = 0; i < arms.size(); i++) {
                Object arm = arms.get(i);
                Object pArm = protocol.arms.get(i);
                if (!arm.equals(pArm)) {
                    return false;
                }
            }
        }
        // DO NOT equate parentArm ...infinite recursive loops on every invocation of any multi-arm protocol
        if (visitTypes != null && protocol.visitTypes != null) {
            if(visitTypes.size() != protocol.visitTypes.size()) return false;
            for (int i = 0; i < visitTypes.size(); i++) {
                Object vt = visitTypes.get(i);
                Object pVt = protocol.visitTypes.get(i);
                if (!vt.equals(pVt)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (versionDescription != null ? versionDescription.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (visitTypes != null ? visitTypes.hashCode() : 0);
        result = 31 * result + (allowUnexpectedExperiments ? 1 : 0);
        result = 31 * result + (unexpectedExperimentsRequireApproval ? 1 : 0);
        result = 31 * result + (allowMultipleOpenVisits ? 1 : 0);
        result = 31 * result + (allowExceptions ? 1 : 0);
        result = 31 * result + (allowedExceptions != null ? allowedExceptions.hashCode() : 0);
//        result = 31 * result + (ongoingExperiments != null ? ongoingExperiments.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Protocol that) {
        return this.name.compareToIgnoreCase(that.name);
    }

    // This method should be called before updating the protocol object to prevent Hibernate from merely updating
    // the existing VisitType, ExpectedExperiment and ExpectedAssessor table rows, but forcing it to create brand
    // new ones for each new version of the protocol object that's saved so that it doesn't update the foreign key
    // to existing rows which would effectively break the link to previous versions of the protocol that are likely
    // being used by existing projects.
    public void cleanChildIds() {
        for (VisitType vt : visitTypes) {
            vt.setId(0);
            for (ExpectedExperiment ee : vt.getExpectedExperiments()) {
                ee.setId(0);
                for (ExpectedAssessor ea : ee.getExpectedAssessors()) {
                    ea.setId(0);
                }
            }
        }
    }
}

