/*
 * protocols: org.nrg.xnat.protocol.entities.subentities.VisitType
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.entities.subentities;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;
import org.nrg.xnat.protocol.entities.Protocol;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.*;

@Auditable
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class VisitType extends AbstractHibernateEntity implements Comparable<VisitType> {
    private Protocol protocol;
    private String description;
    private List<ExpectedExperiment> expectedExperiments = new ArrayList<>();
    private String name;
    private boolean initial = false;
    private boolean terminal = false;
    private int sortOrder;
    private int delta;
    private int deltaDrift;
    private int deltaLow;
    private int deltaHigh;
    private ArrayList<String> nextVisits = new ArrayList<>();

    public VisitType() {
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

    public VisitType(String name, ArrayList<ExpectedExperiment> expectedExperiments, String description, boolean terminal) {
        this();
        this.description = description;
        this.expectedExperiments = expectedExperiments;
        this.name = name;
        this.terminal = terminal;
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @OneToMany(mappedBy = "visitType", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    public List<ExpectedExperiment> getExpectedExperiments() {
        return this.expectedExperiments;
    }

    public void setExpectedExperiments(List<ExpectedExperiment> expectedExperiments) {
        if(expectedExperiments != null) {
            this.expectedExperiments = expectedExperiments;
            Protocol prot = this.getProtocol();
            for(ExpectedExperiment ee: expectedExperiments){
                ee.setVisitType(this);
                ee.setProtocol(prot);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean getInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public int getDeltaDrift() {
        return deltaDrift;
    }

    public void setDeltaDrift(int deltaDrift) {
        this.deltaDrift = deltaDrift;
    }

    public int getDeltaHigh() {
        return deltaHigh;
    }

    public void setDeltaHigh(int deltaHigh) {
        this.deltaHigh = deltaHigh;
    }

    public int getDeltaLow() {
        return deltaLow;
    }

    public void setDeltaLow(int deltaLow) {
        this.deltaLow = deltaLow;
    }

    public ArrayList<String> getNextVisits() {
        if (this.nextVisits == null) nextVisits = new ArrayList<>();
        return nextVisits;
    }

    public void setNextVisits(ArrayList<String> nextVisits) {
        this.nextVisits = nextVisits;
    }

    public boolean isExpectedExperiment(String type, String protocol) {
        for(ExpectedExperiment ee : expectedExperiments) {
            if (ee.getType().equals(type) && ((StringUtils.isEmpty(ee.getSubtype()) && StringUtils.isEmpty(protocol)) ||
                    (ee.getSubtype() != null && ee.getSubtype().equals(protocol)))) return true;
        }
        return false;
    }

    public ExpectedExperiment getExpectedExperiment(String type, String protocol) {
        for(ExpectedExperiment ee : expectedExperiments) {
            if (ee.getType().equals(type) && ((StringUtils.isEmpty(ee.getSubtype()) && StringUtils.isEmpty(protocol)) ||
                    (ee.getSubtype() != null && ee.getSubtype().equals(protocol)))) return ee;
        }
        return null;
    }

    public String validate(List<VisitType> visitTypes) {
        StringBuffer errors = new StringBuffer();
        ArrayList<String> experimentsPairs = new ArrayList<>();
        for (ExpectedExperiment ee : expectedExperiments) {
            if (experimentsPairs.contains(ee.getType() + ee.getSubtype())) {
                errors.append("One or more expected experiments for visit type " + name + " cannot be differentiated. Add a 'subtype' to expected experiments that share a type.\n");
            }
            experimentsPairs.add(ee.getType() + ee.getSubtype());
        }

        Set<String> visitTypeStrings = new HashSet<>();
        for (VisitType visitType : visitTypes) {
            visitTypeStrings.add(visitType.getName());
        }

        for (String nextVisit : nextVisits) {
            if (!visitTypeStrings.contains(nextVisit)) {
                errors.append(nextVisit + " was specified in " + name + " as a valid subsequent visit, but it was not defined as a VisitType.\n");
            }
        }

        return errors.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisitType)) return false;
        VisitType visitType = (VisitType) o;
        if (initial != visitType.initial) return false;
        if (terminal != visitType.terminal) return false;
        if (sortOrder != visitType.sortOrder) return false;
        if (delta != visitType.delta) return false;
        if (description != null ? !description.equals(visitType.description) : visitType.description != null) return false;
        if (expectedExperiments != null && visitType.expectedExperiments != null) {
            if(expectedExperiments.size() != visitType.expectedExperiments.size()) return false;
            for (int i = 0; i < expectedExperiments.size(); i++) {
                Object exp = expectedExperiments.get(i);
                Object vtExp = visitType.expectedExperiments.get(i);
                if(!exp.equals(vtExp)) {
                    return false;
                }
            }
        }
        if (name != null ? !name.equals(visitType.name) : visitType.name != null) return false;
        if (nextVisits != null ? !nextVisits.equals(visitType.nextVisits) : visitType.nextVisits != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) this.getId();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (expectedExperiments != null ? expectedExperiments.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (initial ? 1 : 0);
        result = 31 * result + (terminal ? 1 : 0);
        result = 31 * result + delta;
        result = 31 * result + deltaDrift;
        result = 31 * result + (nextVisits != null ? nextVisits.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(VisitType that) {
        return this.name.compareToIgnoreCase(that.name);
    }
}
