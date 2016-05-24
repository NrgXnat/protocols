package org.nrg.xnat.protocol.util;/*
 * org.nrg.xnat.protocol.util.SubjectVisitInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/12/13 1:27 PM
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.TableSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.ProtocolException;
import org.nrg.xnat.protocol.entities.subentities.ExpectedAssessor;
import org.nrg.xnat.protocol.entities.subentities.ExpectedExperiment;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.services.ProtocolExceptionService;

import javax.inject.Inject;
import java.util.*;

public class SubjectVisitInfo {
    private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SubjectVisitInfo.class);
    private ProtocolExceptionService _service;
    private XnatSubjectdata subject;
    private String currentProject;
    private String projectList;
    private UserI user;
    private Set<String> usedVisitIds = new HashSet<String>();
    private Set<String> usedExperimentIds = new HashSet<String>();
    private Set<String> usedAssessorIds = new HashSet<String>();
    private List<ExpectedExperiment> floatingExperiments = new ArrayList<ExpectedExperiment>();
    private ProtocolVisitSubjectHelper protocolHelper = new ProtocolVisitSubjectHelper();

    // returnable data
    private Protocol protocol;
    private List<ExperimentInfo> unsortedExperiments = new ArrayList<ExperimentInfo>();
//    private List<ExperimentInfo> ongoingExperiments = new ArrayList<ExperimentInfo>();
    private List<VisitInfo> visits = new ArrayList<VisitInfo>();
    private List<VisitInfo> expectedVisits = new ArrayList<VisitInfo>();
    private List<VisitInfo> unexpectedVisits = new ArrayList<VisitInfo>();
    private boolean hasOpenVisit = false;

    public SubjectVisitInfo(XnatSubjectdata subject, String projectId, UserI user) throws Exception {
        this.subject = subject;
        this.currentProject = projectId;
        this.user = user;
        protocol = protocolHelper.getProtocol(currentProject, user);
        projectList = populateProjectList(currentProject); // method requires subject, user, and protocol; must go last

        if (protocol != null) {
            populateVisits(); // must be populated first
//            ongoingExperiments.addAll(populateOngoingExperiments());
            unsortedExperiments.addAll(populateUnsortedExperiments()); // requires visits to be initialized first
        }
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public List<ExperimentInfo> getUnsortedExperiments() {
        return unsortedExperiments;
    }

//    public List<ExperimentInfo> getOngoingExperiments() {
//        return ongoingExperiments;
//    }

    public List<VisitInfo> getVisits() {
        return visits;
    }
    public List<VisitInfo> getExpectedVisits() {
        return expectedVisits;
    }
    public List<VisitInfo> getUnexpectedVisits() {
        return unexpectedVisits;
    }

    private String populateProjectList(String projectId) {
        String projectList = "'" + projectId + "'";

        if (!subject.getProject().equals(projectId)) {
            Protocol originProtocol = protocolHelper.getProtocol(subject.getProject(), user); // protocol of the project the subject comes from

            // protocol equivalency is needed to share visits, and is assessed by matching protocol lineage
            if (originProtocol != null &&
                    originProtocol.getProtocolId().equals(protocol.getProtocolId())) {
                projectList += ",'" + subject.getProject() + "'";
            }
        }

        for (XnatProjectparticipantI pp : subject.getSharing_share()) {
            String sharedProject = pp.getProject();

            Protocol sharedProtocol = protocolHelper.getProtocol(sharedProject, user); // protocol of the project the subject is shared with

            // protocol equivalency is needed to share visits, and is assessed by matching protocol lineage
            if (sharedProtocol != null &&
                    sharedProtocol.getProtocolId().equals(protocol.getProtocolId())) {
                projectList += ",'" + sharedProject + "'";
            }
        }
        return projectList;
    }

    private List<ExperimentInfo> populateUnsortedExperiments() throws Exception {
        usedAssessorIds = new HashSet<String>(); // empty list so no assessors are filtered out
        List<ExperimentInfo> unsortedExperiments = new ArrayList<ExperimentInfo>();

        XFTTable table = TableSearch.Execute("SELECT ex.id, me.element_name AS type, " +
                "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                "FROM xnat_subjectAssessorData sub " +
                "INNER JOIN xnat_experimentData ex ON ex.id=sub.id " +
                "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                "LEFT JOIN xnat_imageAssessorData ia ON ex.id=ia.id " +
                "WHERE sub.subject_id = '" + subject.getId() + "' " +
                "AND ex.visit IS NULL " +
                "AND ia.imagesession_id IS NULL " +
                "ORDER BY ex.date ASC;",
                null, null);
        table.resetRowCursor();
        while (table.hasMoreRows()) {
            final Hashtable row = table.nextRowHash();

            ExperimentInfo experimentInfo = new ExperimentInfo();
            String id = (String) row.get("id");
            if (usedExperimentIds.add(id) && XnatExperimentdata.getXnatExperimentdatasById(id, user, false).hasProject(currentProject)) {
                experimentInfo.experiment = XnatExperimentdata.getXnatExperimentdatasById(id, user, false);
                experimentInfo.type = (String) row.get("type");
                experimentInfo.subtype = (String) row.get("subtype");
                experimentInfo.required = false;
                experimentInfo.assessors.addAll(getAssessorsForExperiment(id));
                experimentInfo.validVisits.addAll(getValidVisitsForExperiment(experimentInfo.type, experimentInfo.subtype));
                unsortedExperiments.add(experimentInfo);
            }
        }

        return unsortedExperiments;
    }
/*
    private Collection<ExperimentInfo> populateOngoingExperiments() throws Exception {
        List<ExperimentInfo> experiments = new ArrayList<ExperimentInfo>();

        for (ExpectedExperiment expectedExperiment : protocol.getOngoingExperiments()) {
            String subtypeClause = "";
            if (StringUtils.isEmpty(expectedExperiment.getSubtype())) {
                subtypeClause += "AND (ex.protocol IS NULL OR ex.protocol = '') ";
            }
            else {
                subtypeClause += "AND ex.protocol = '" + expectedExperiment.getSubtype().replace("\\", "\\\\") + "' ";
            }

            XFTTable table = TableSearch.Execute("SELECT ex.id, me.element_name AS type, " +
                    "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                    "FROM xnat_subjectAssessorData sub " +
                    "INNER JOIN xnat_experimentData ex ON ex.id=sub.id " +
                    "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                    "WHERE sub.subject_id = '" + subject.getId() + "' " +
                    "AND ex.visit IS NULL " +
                    subtypeClause +
                    "AND me.element_name = '" + expectedExperiment.getType() + "' " +
                    "ORDER BY ex.date ASC;",
                    null, null);
            table.resetRowCursor();
            while (table.hasMoreRows()) {
                final Hashtable row = table.nextRowHash();

                ExperimentInfo experimentInfo = new ExperimentInfo();
                String id = (String) row.get("id");
                if (usedExperimentIds.add(id) && XnatExperimentdata.getXnatExperimentdatasById(id, user, false).hasProject(currentProject)) {
                    experimentInfo.experiment = XnatExperimentdata.getXnatExperimentdatasById(id, user, false);
                    experimentInfo.type = (String) row.get("type");
                    experimentInfo.subtype = (String) row.get("subtype");
                    experimentInfo.required = false;
                    experimentInfo.assessors.addAll(getExpectedAssessorsForExperiment("null", id, expectedExperiment, false));
                    experiments.add(experimentInfo);
                }
            }
            ExperimentInfo experimentInfo = new ExperimentInfo();
            experimentInfo.type = expectedExperiment.getType();
            experimentInfo.subtype = expectedExperiment.getSubtype();
            experimentInfo.required = false;
            experimentInfo.imageSession = XFTItem.NewItem(expectedExperiment.getType(), user).instanceOf("xnat:imageSessionData");
            experiments.add(experimentInfo);
        }
        return experiments;
    }
*/
    private void populateVisits() throws Exception {
        boolean pastLastValidVisit = false;
        VisitInfo initialExpectedVisit = null;
        VisitInfo lastValidVisit = null;
        VisitType visitType = null;
        //List<String> validVisits = new ArrayList<String>();
        XFTTable table = TableSearch.Execute("SELECT v.id, v.visit_type AS type, " +
                "ex.date, v.closed, v.terminal, v.protocolVersion, v.protocolId " +
                "FROM xnat_pVisitData v " +
                "INNER JOIN xnat_experimentData ex ON ex.id=v.id " +
                "WHERE ex.project IN (" + projectList + ") " +
                "AND v.subject_id = '" + subject.getId() + "' " +
                "ORDER BY ex.date ASC;",
                null, null);
        table.resetRowCursor();
        while (table.hasMoreRows()) {
            final Hashtable row = table.nextRowHash();
            String id = (String) row.get("id");
            XnatPvisitdata visit = XnatPvisitdata.getXnatPvisitdatasById(id, user, false);
            VisitInfo visitInfo = new VisitInfo();
            visitInfo.id = visit.getId();
            visitInfo.name = visit.getVisitName();
            visitInfo.type = visit.getVisitType();
            visitInfo.date = (Date) visit.getDate();
            visitInfo.closed = visit.getClosed();
            visitInfo.terminal = visit.getTerminal();
            visitInfo.userCanEdit = visit.canEdit(user);

            visitType = protocol.getVisitType(visitInfo.getType());
            if (visitType != null) { // safeguarding against aggressive protocol changes, these will only apply to valid visits
                visitInfo.description = visitType.getDescription();
                // visitInfo.nextVisits = visitType.getNextVisits();
                visitInfo.nextVisitType = getNextVisitType(visitType);
                visitInfo.experiments.addAll(getExpectedExperimentsForVisit(id, visitType, visitInfo.getClosed()));
                visitInfo.experiments.addAll(getFloatingExperimentsForVisit(id, visitInfo.getClosed()));
                visitInfo.unexpectedExperiments.addAll(getExperimentsForVisit(id));
                //visitInfo.valid = validVisits.contains(visitInfo.type) || (validVisits.isEmpty() && visitType.getInitial());

                visitInfo.requirementsFilled = true;
                for (ExperimentInfo experimentInfo : visitInfo.experiments) {
                    if (experimentInfo.required && experimentInfo.experiment == null) {
                        visitInfo.requirementsFilled = false;
                    } else {
                        for (AssessorInfo assessorInfo : experimentInfo.assessors) {
                            if (assessorInfo.required && assessorInfo.assessor == null) {
                                visitInfo.requirementsFilled = false;
                            }
                        }
                    }
                }
                if (!protocol.getAllowUnexpectedExperiments() && visitInfo.unexpectedExperiments.size() > 0) {
                    visitInfo.requirementsFilled = false;
                }

                // if visit visibility needs to be restricted (due to sharing issues or whatnot), this is the place to do it
                // visits will only be shown past the last valid visit if they have already been created
                if (!pastLastValidVisit) {
                    lastValidVisit = visitInfo;
                    visits.add(visitInfo);
                } else {
                    unexpectedVisits.add(visitInfo);
                }

                if (visitType.getTerminal() || visitInfo.nextVisitType == null) {
                    pastLastValidVisit = true;
                }
            } else {
                visitInfo.experiments.addAll(getExperimentsForVisit(id));
                if(visitInfo.getTerminal()){
                    visitInfo.valid = true;
                    visits.add(visitInfo);
                    pastLastValidVisit = true;
                } else if(protocol.getAllowUnexpectedAdHocVisits() && visitInfo.getType() == null){
                    visitInfo.adHoc = true;
                    visitInfo.valid = true;
                    visits.add(visitInfo);
                } else {
                    unexpectedVisits.add(visitInfo);
                }
            }

            if (!visitInfo.getClosed()) {
                hasOpenVisit = true;
            }

            if(initialExpectedVisit == null){
                initialExpectedVisit = visitInfo; // Since the query above is sorted ascending by the subject's visit dates, their first one by definition will come first and be set here.
            }
        }
        if(!pastLastValidVisit) {
            VisitType nextExpectedVisitType;
            VisitInfo visitInfo = new VisitInfo();
            if(lastValidVisit != null) {
                nextExpectedVisitType = lastValidVisit.getNextVisitType();
                if(nextExpectedVisitType != null) {
                    Calendar c = Calendar.getInstance();
                    // Might need future logic here to determine how we're interpreting the delta:
                    //    1. as the offset from the subject's first baseline visit date ...or...
                    //    2. as the offset from the subject's very last visit (which will likely not be consistent with the protocol)
                    //       (probably want a configuration switch in the Protocol Management UI explaining these interpretation distinctions and allowing them to select one or the other)
                    // Going with assumption #1 for now!
                    if (initialExpectedVisit != null && initialExpectedVisit.getDate() != null) {
                        c.setTime(initialExpectedVisit.getDate());
                    }
                    // ...otherwise interpretation #2 would look like this...
                    // c.setTime(lastValidVisit.getDate());
                    c.add(Calendar.DATE, nextExpectedVisitType.getDelta());
                    visitInfo.date = c.getTime();  // set this to the ideal expected visit date

                    // TODO: Also set the earliest possibly date for the subject to come in
                    // TODO: ...and the the latest ...for guidelines to remain within protocol ...in new SubjectVisitInfo .earliestSchedulableDate AND .latestSchedulableDate fields
                }

            } else { // no visits currently exist ...brand new subject??
                List<VisitType> visitTypes = protocol.getVisitTypes();
                if(visitTypes != null && visitTypes.size() > 0) {
                    nextExpectedVisitType = visitTypes.get(0);
                } else {
                    return; // protocol has no expected visits defined so it's effectively not worth showing anything
                    // unless we want to allow ad hoc visits in this case as well?
                }
                visitInfo.date = new Date();  // set this to today!
            }

            //TODO: mm/dd/yyyy Date Format for expected date

            visitInfo.id = null;
            visitInfo.name = null;
            visitInfo.type = nextExpectedVisitType.getName();
            visitInfo.closed = false;
            visitInfo.terminal = nextExpectedVisitType.getTerminal();
            visitInfo.userCanEdit = true;       // Maybe?? I'm not sure about this
            visitInfo.description = nextExpectedVisitType.getDescription();
            visitInfo.requirementsFilled = false;
            visitInfo.valid = true;
            visitInfo.nextVisitType = getNextVisitType(nextExpectedVisitType);
            List <ExpectedExperiment> ees = nextExpectedVisitType.getExpectedExperiments();
            for(ExpectedExperiment ee : ees) {
                ExperimentInfo ei = new ExperimentInfo();
                ei.type = ee.getType();
                ei.subtype = ee.getSubtype();
                ei.required = ee.getRequired();
                List<ExpectedAssessor> eas = ee.getExpectedAssessors();
                for (ExpectedAssessor ea : eas) {
                    AssessorInfo ai = new AssessorInfo();
                    ai.type = ea.getType();
                    ai.subtype = ea.getSubtype();
                    ai.required = ea.getRequired();
                    ei.assessors.add(ai);
                }
                visitInfo.experiments.add(ei);
            }
            //visitInfo.experiments.addAll(getFloatingExperimentsForVisit(id, visitInfo.getClosed()));    // Suggest existing experients to add to this new visit we're about to create that roughly match the expected date, type and subtype requirements??
            expectedVisits.add(visitInfo);
        }
    }

    private VisitType getNextVisitType(VisitType thisVt){
        VisitType nextVt = null;
        if(thisVt != null) {
            List vts = protocol.getVisitTypes();
            if(vts != null && vts.size() > thisVt.getSortOrder()) {
                int nextVtIndex = thisVt.getSortOrder() + 1;
                if(nextVtIndex < vts.size()) {
                    nextVt = (VisitType) vts.get(nextVtIndex);
                }
            }
        }
        return nextVt;
    }

    private List<ExperimentInfo> getExpectedExperimentsForVisit(String visitId, VisitType visitType, boolean visitClosed) throws Exception {
        List<ExperimentInfo> experiments = new ArrayList<ExperimentInfo>();

        for (ExpectedExperiment expectedExperiment : visitType.getExpectedExperiments()) {

            String subtypeClause = "";
            if (StringUtils.isEmpty(expectedExperiment.getSubtype())) {
                subtypeClause += "AND (ex.protocol IS NULL OR ex.protocol = '') ";
            }
            else {
                subtypeClause += "AND ex.protocol = '" + expectedExperiment.getSubtype().replace("\\", "\\\\") + "' ";
            }

            XFTTable table = TableSearch.Execute("SELECT ex.id, me.element_name AS type, " +
                    "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                    "FROM xnat_subjectAssessorData sub " +
                    "INNER JOIN xnat_experimentData ex ON ex.id=sub.id " +
                    "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                    "WHERE sub.subject_id = '" + subject.getId() + "' " +
                    "AND ex.visit = '" + visitId + "' " +
                    subtypeClause +
                    "AND me.element_name = '" + expectedExperiment.getType() + "' " +
//                    "AND ex.id NOT IN (" + commaSeperatedList(usedExperimentIds) + ") " +
                    "ORDER BY ex.date ASC;",
                    null, null);
            table.resetRowCursor();
            if (table.hasMoreRows()) {
                do {
                    final Hashtable row = table.nextRowHash();

                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    String id = (String) row.get("id");
                    if (usedExperimentIds.add(id) && XnatExperimentdata.getXnatExperimentdatasById(id, user, false).hasProject(currentProject)) {
                        experimentInfo.experiment = XnatExperimentdata.getXnatExperimentdatasById(id, user, false);
                        experimentInfo.type = (String) row.get("type");
                        experimentInfo.subtype = (String) row.get("subtype");
                        experimentInfo.required = false;
                        experimentInfo.assessors.addAll(getExpectedAssessorsForExperiment(visitId, id, expectedExperiment, visitClosed));
                        experimentInfo.unexpectedAssessors.addAll(getAssessorsForExperiment(id));
                        experiments.add(experimentInfo);
                    }
                } while (expectedExperiment.getAcceptMultiple() && table.hasMoreRows());
                if (!visitClosed && expectedExperiment.getAcceptMultiple()) {
                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    experimentInfo.type = expectedExperiment.getType();
                    experimentInfo.subtype = expectedExperiment.getSubtype();
                    experimentInfo.required = false;
                    experimentInfo.imageSession = XFTItem.NewItem(expectedExperiment.getType(), user).instanceOf("xnat:imageSessionData");
                    experiments.add(experimentInfo);
                }
            }
            else {
                ProtocolException protocolException =
                        _protocolExceptionService.findExceptionForVisitAndType(visitId, expectedExperiment.getType(), expectedExperiment.getSubtype());
                if (protocol.getAllowExceptions() && protocolException != null) {
                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    experimentInfo.experiment = protocolException;
                    experimentInfo.type = protocolException.get_xsiType();
                    experimentInfo.subtype = protocolException.get_subtype();
                    experimentInfo.required = false;
                    experiments.add(experimentInfo);
                }
                else if (!visitClosed) {
                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    experimentInfo.type = expectedExperiment.getType();
                    experimentInfo.subtype = expectedExperiment.getSubtype();
                    experimentInfo.required = expectedExperiment.getRequired();
                    experimentInfo.imageSession = XFTItem.NewItem(expectedExperiment.getType(), user).instanceOf("xnat:imageSessionData");
//                    experimentInfo.assessors.addAll(getExpectedAssessorsForExperiment(visitId, null, expectedExperiment, visitClosed)); // still get required assessors
                    experiments.add(experimentInfo);
                }
                else if (expectedExperiment.getRequired()) {
                    floatingExperiments.add(expectedExperiment);
                }
            }
        }
        return experiments;
    }

    List<String> noCreationLinks = Arrays.asList("fs:fsData", "rad:radiologyReadData");
    private boolean showCreationLink(String type) {
        if(type != null) {
            for (String noLinks : noCreationLinks) {
                if (noLinks.equals(type)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private List<ExperimentInfo> getExperimentsForVisit(String visitId) throws Exception {
        List<ExperimentInfo> experiments = new ArrayList<ExperimentInfo>();

        XFTTable table = TableSearch.Execute("SELECT ex.id, me.element_name AS type, " +
                "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                "FROM xnat_subjectAssessorData sub " +
                "INNER JOIN xnat_experimentData ex ON ex.id=sub.id " +
                "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                "LEFT JOIN xnat_imageAssessorData ia ON ex.id=ia.id " +
                "WHERE sub.subject_id = '" + subject.getId() + "' " +
                "AND ex.visit = '" + visitId + "' " +
                "AND ia.imagesession_id IS NULL " +
                "ORDER BY ex.date ASC;",
                null, null);
        table.resetRowCursor();
        while (table.hasMoreRows()) {
            final Hashtable row = table.nextRowHash();

            ExperimentInfo experimentInfo = new ExperimentInfo();
            String id = (String) row.get("id");
            if (usedExperimentIds.add(id) && XnatExperimentdata.getXnatExperimentdatasById(id, user, false).hasProject(currentProject)) {
                experimentInfo.experiment = XnatExperimentdata.getXnatExperimentdatasById(id, user, false);
                experimentInfo.type = (String) row.get("type");
                experimentInfo.subtype = (String) row.get("subtype");
                experimentInfo.required = false;
                experimentInfo.assessors.addAll(getAssessorsForExperiment(id));
                experiments.add(experimentInfo);
            }
        }
        return experiments;
    }

    private List<ExperimentInfo> getFloatingExperimentsForVisit(String visitId, boolean visitClosed) throws Exception {
        List<ExperimentInfo> experiments = new ArrayList<ExperimentInfo>();
        List<ExpectedExperiment> experimentsToRemove = new ArrayList<ExpectedExperiment>();

        for (ExpectedExperiment floatingExperiment : floatingExperiments) {
            String subtypeClause = "";
            if (StringUtils.isEmpty(floatingExperiment.getSubtype())) {
                subtypeClause += "AND (ex.protocol IS NULL OR ex.protocol = '') ";
            }
            else {
                subtypeClause += "AND ex.protocol = '" + floatingExperiment.getSubtype().replace("\\", "\\\\") + "' ";
            }


            XFTTable table = TableSearch.Execute("SELECT ex.id, me.element_name AS type, " +
                    "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                    "FROM xnat_subjectAssessorData sub " +
                    "INNER JOIN xnat_experimentData ex ON ex.id=sub.id " +
                    "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                    "WHERE sub.subject_id = '" + subject.getId() + "' " +
                    "AND ex.visit = '" + visitId + "' " +
                    subtypeClause +
                    "AND me.element_name = '" + floatingExperiment.getType() + "' " +
//                        "AND ex.id NOT IN (" + commaSeperatedList(usedExperimentIds) + ") " +
                    "ORDER BY ex.date ASC;",
                    null, null);
            table.resetRowCursor();
            if (table.hasMoreRows()) {
                final Hashtable row = table.nextRowHash();

                ExperimentInfo experimentInfo = new ExperimentInfo();
                String id = (String) row.get("id");
                if (usedExperimentIds.add(id) && XnatExperimentdata.getXnatExperimentdatasById(id, user, false).hasProject(currentProject)) {
                    experimentInfo.experiment = XnatExperimentdata.getXnatExperimentdatasById(id, user, false);
                    experimentInfo.type = (String) row.get("type");
                    experimentInfo.subtype = (String) row.get("subtype");
                    experimentInfo.required = false;
                    experimentInfo.floating = true;
                    experimentInfo.assessors.addAll(getExpectedAssessorsForExperiment(visitId, id, floatingExperiment, visitClosed));
                    experimentInfo.unexpectedAssessors.addAll(getAssessorsForExperiment(id));
                    experiments.add(experimentInfo);
                }
                experimentsToRemove.add(floatingExperiment);
            }
            else {
                ProtocolException protocolException =
                        _protocolExceptionService.findExceptionForVisitAndType(visitId, floatingExperiment.getType(), floatingExperiment.getSubtype());
                if (protocol.getAllowExceptions() && protocolException != null) {
                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    experimentInfo.experiment = protocolException;
                    experimentInfo.type = protocolException.get_xsiType();
                    experimentInfo.subtype = protocolException.get_subtype();
                    experimentInfo.required = false;
                    experimentInfo.floating = true;
                    experiments.add(experimentInfo);
                    experimentsToRemove.add(floatingExperiment);
                }
                else if (!visitClosed) {
                    ExperimentInfo experimentInfo = new ExperimentInfo();
                    experimentInfo.type = floatingExperiment.getType();
                    experimentInfo.subtype = floatingExperiment.getSubtype();
                    experimentInfo.required = floatingExperiment.getRequired();
                    experimentInfo.floating = true;
                    experimentInfo.imageSession = XFTItem.NewItem(floatingExperiment.getType(), user).instanceOf("xnat:imageSessionData");
//                        experimentInfo.assessors.addAll(getExpectedAssessorsForExperiment(visitId, null, expectedExperiment, visitClosed)); // still get required assessors
                    experiments.add(experimentInfo);
                    experimentsToRemove.add(floatingExperiment);
                }
            }
        }

        for (ExpectedExperiment experimentToRemove : experimentsToRemove) {
            floatingExperiments.remove(experimentToRemove);
        }

        return experiments;
    }

    private List<AssessorInfo> getExpectedAssessorsForExperiment(String visitId, String experimentId, ExpectedExperiment experiment, boolean visitClosed) throws Exception {
        List<AssessorInfo> assessors = new ArrayList<AssessorInfo>();

        for (ExpectedAssessor expectedAssessor : experiment.getExpectedAssessors()) {
            XFTTable table = TableSearch.Execute("SELECT  ex.id, me.element_name AS type, " +
                    "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                    "FROM xnat_imageAssessorData im " +
                    "INNER JOIN xnat_experimentData ex ON ex.id=im.id " +
                    "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                    "WHERE im.imageSession_id = '" + experimentId + "' " +
                    "AND me.element_name = '" + expectedAssessor.getType() + "' " +
//                    "AND ex.id NOT IN (" + commaSeperatedList(usedAssessorIds) + ") " +
                    "ORDER BY ex.date ASC;",
                    null, null);
            table.resetRowCursor();
            if (table.hasMoreRows()) {
                do {
                    final Hashtable row = table.nextRowHash();

                    AssessorInfo assessorInfo = new AssessorInfo();
                    String id = (String) row.get("id");
                    if (usedAssessorIds.add(id) &&  XnatImageassessordata.getXnatImageassessordatasById(id, user, false).hasProject(currentProject)) {
                        assessorInfo.assessor = XnatImageassessordata.getXnatImageassessordatasById(id, user, false);
                        assessorInfo.type = (String) row.get("type");
                        assessorInfo.subtype = (String) row.get("subtype");
                        assessorInfo.required = false;
                        assessors.add(assessorInfo);
                    }
                } while (expectedAssessor.getAcceptMultiple() && table.hasMoreRows());
                if (!visitClosed && expectedAssessor.getAcceptMultiple()) {
                    AssessorInfo assessorInfo = new AssessorInfo();
                    assessorInfo.type = expectedAssessor.getType();
                    assessorInfo.subtype = expectedAssessor.getSubtype();
                    assessorInfo.action = "XDATScreen_edit_" + GenericWrapperElement.GetElement(expectedAssessor.getType()).getSQLName();
                    assessorInfo.required = false;
                    assessors.add(assessorInfo);
                }
            }
            else {
                ProtocolException protocolException =
                        _protocolExceptionService.findExceptionForVisitAndType(visitId, expectedAssessor.getType(), experimentId);
                if (protocol.getAllowExceptions() && protocolException != null) {
                    AssessorInfo assessorInfo = new AssessorInfo();
                    assessorInfo.assessor = protocolException;
                    assessorInfo.type = protocolException.get_xsiType();
                    assessorInfo.subtype = protocolException.get_subtype();
                    assessorInfo.required = false;
                    assessors.add(assessorInfo);
                }
                else if (!visitClosed) {
                    AssessorInfo assessorInfo = new AssessorInfo();
                    assessorInfo.type = expectedAssessor.getType();
                    assessorInfo.subtype = expectedAssessor.getSubtype();
                    assessorInfo.action = "XDATScreen_edit_" + GenericWrapperElement.GetElement(expectedAssessor.getType()).getSQLName();
                    assessorInfo.required = expectedAssessor.getRequired();
                    assessors.add(assessorInfo);
                }
            }
        }
        return assessors;
    }

    private List<AssessorInfo> getAssessorsForExperiment(String experimentId) throws Exception {
        List<AssessorInfo> assessors = new ArrayList<AssessorInfo>();

        XFTTable table = TableSearch.Execute("SELECT  ex.id, me.element_name AS type, " +
                "ex.protocol AS subtype, ex.label, ex.project, ex.date " +
                "FROM xnat_imageAssessorData im " +
                "INNER JOIN xnat_experimentData ex ON ex.id=im.id " +
                "INNER JOIN xdat_meta_element me ON ex.extension = me.xdat_meta_element_id " +
                "WHERE im.imageSession_id = '" + experimentId + "' " +
                "ORDER BY ex.date ASC;",
                null, null);
        table.resetRowCursor();
        while (table.hasMoreRows()) {
            final Hashtable row = table.nextRowHash();

            AssessorInfo assessorInfo = new AssessorInfo();
            String id = (String) row.get("id");

            if (usedAssessorIds.add(id) &&  XnatImageassessordata.getXnatImageassessordatasById(id, user, false).hasProject(currentProject)) { // only if this assessor has not already been accounted for
                assessorInfo.assessor = XnatImageassessordata.getXnatImageassessordatasById(id, user, false);
                assessorInfo.type = (String) row.get("type");
                assessorInfo.subtype = (String) row.get("subtype");
                assessorInfo.required = false;
                assessors.add(assessorInfo);
            }
        }

        return assessors;
    }

    public List<VisitInfo> getValidVisitsForExperiment(String type, String subtype) {
        List<VisitInfo> validVisits = new ArrayList<VisitInfo>();

        for (VisitInfo visitInfo : visits) {
            if (!visitInfo.getClosed()) { // visit to add to must be open
                // any open visit will do if we're not restricted (and an owner)
                if (protocol.getAllowUnexpectedExperiments() && (UserHelper.getUserHelperService(user).isOwner(subject.getProject()) || Roles.isSiteAdmin(user))) {
                    validVisits.add(visitInfo);
                }
                else {
                    // look through the missing experiments of each visit for one with matching type and subtype
                    for (ExperimentInfo expectedExperiment : visitInfo.experiments) {
                        String expectedSubtype = expectedExperiment.subtype != null ? expectedExperiment.subtype : "";
                        String potentialSubtype = subtype != null ? subtype : "";
                        if ((expectedExperiment.experiment == null) &&
                                expectedExperiment.type.equals(type) &&
                                ObjectUtils.equals(expectedSubtype, potentialSubtype)) { // ObjectUtils will obviate problems with either subtype object being null
                            validVisits.add(visitInfo);
                        }
                    }
                }
            }
        }

        return validVisits;
    }

    public boolean getHasOpenVisit() {
        return hasOpenVisit;
    }

    public String commaSeperatedList(Collection collection) {
        StringBuilder sb = new StringBuilder();
        for (Object item : collection) {
            if (item instanceof String) {
                sb.append("'");
            }
            sb.append(item.toString());
            if (item instanceof String) {
                sb.append("'");
            }
            sb.append(",");
        }
        // remove dangling comma
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public class VisitInfo {
        String id;
        String name;
        String type;
        String description;
        Date date;
        Boolean closed;
        Boolean adHoc = false;
        Boolean terminal = false;
        Boolean valid;
//        List<String> nextVisits = new ArrayList<String>();
        VisitType nextVisitType = null;
        List<ExperimentInfo> experiments = new ArrayList<ExperimentInfo>();
        List<ExperimentInfo> unexpectedExperiments = new ArrayList<ExperimentInfo>();
        Boolean requirementsFilled = true;
        Boolean userCanEdit = false;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public Date getDate() {
            return date;
        }

        public String getDateFormatted() {
            if(this.date != null) {
                return DateUtils.format(this.date, "MM/dd/yyyy");
            }
            return "";
        }

        public Boolean getClosed() {
            return closed;
        }

        public Boolean getAdHoc() { return adHoc; }

        public Boolean getTerminal() { return terminal; }

        public Boolean getValid() {
            return valid;
        }

//        public List<String> getNextVisits() { return nextVisits; }
        public VisitType getNextVisitType() { return nextVisitType; }

        public Boolean isLastVisit() {
            return nextVisitType == null ? true : false;
        }

        @JsonIgnore
        public List<ExperimentInfo> getExperiments() {
            return experiments;
        }

        @JsonIgnore
        public List<ExperimentInfo> getUnexpectedExperiments() {
            return unexpectedExperiments;
        }

        public Boolean getRequirementsFilled() {
            return requirementsFilled;
        }

        public Boolean getUserCanEdit() {
            return userCanEdit;
        }
    }

    public class ExperimentInfo {
        Object experiment; // can be of type XnatExperimentdata or ProtocolException
        String type;
        String subtype;
        Boolean required;
        Boolean imageSession;
        Boolean floating;
        List<AssessorInfo> assessors = new ArrayList<AssessorInfo>();
        List<AssessorInfo> unexpectedAssessors = new ArrayList<AssessorInfo>();
        List<VisitInfo> validVisits = new ArrayList<VisitInfo>();

        public Object getExperiment() {
            return experiment;
        }

        public String getType() {
            return type;
        }

        public String getSubtype() {
            return subtype;
        }

        public String getEscapedSubtype() {
            return StringEscapeUtils.escapeXml(subtype);
        }

        public Boolean getRequired() {
            return required;
        }

        public Boolean getImageSession() {
            return imageSession;
        }

        public Boolean getCreateLink() {
            return showCreationLink(type);
        }

        public Boolean getFloating() {
            return floating;
        }

        public List<AssessorInfo> getAssessors() {
            return assessors;
        }

        public List<AssessorInfo> getUnexpectedAssessors() {
            return unexpectedAssessors;
        }

        public List<VisitInfo> getValidVisits() {
            return validVisits;
        }
    }

    public class AssessorInfo {
        Object assessor; // can be of type XnatImageassessordata or ProtocolException
        String type;
        String subtype;
        String action;
        Boolean required;

        public Object getAssessor() {
            return assessor;
        }

        public String getType() {
            return type;
        }

        public String getSubtype() {
            return subtype;
        }

        public String getAction() {
            return action;
        }

        public Boolean getRequired() {
            return required;
        }
    }

    @Inject
    private ProtocolExceptionService _protocolExceptionService;
}
