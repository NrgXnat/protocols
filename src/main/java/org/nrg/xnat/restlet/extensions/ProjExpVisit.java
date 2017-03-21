/*
 * protocols: org.nrg.xnat.restlet.extensions.ProjExpVisit
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.extensions;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.om.*;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

@XnatRestlet({"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPERIMENT_ID}/visit/{VISIT_ID_OR_NAME}"})
public class ProjExpVisit extends AbstractProtocolResource {
    private static final Log _log = LogFactory.getLog(ProjExpVisit.class);
    XnatProjectdata project = null;
    XnatSubjectdata subject = null;
    XnatExperimentdata experiment = null;
    XnatPvisitdata visit = null;
    Protocol protocol;
    String subtype = null;

    public ProjExpVisit(Context context, Request request, Response response) throws UnsupportedEncodingException {

        super(context, request, response);

        //we need to grab the project, subject, experiment and visit.
        String projectId = (String) getParameter(request, "PROJECT_ID");
        if (projectId != null) {
            project = XnatProjectdata.getProjectByIDorAlias(projectId, getUser(), false);
        }
        if (project == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Unable to identify project " + projectId, MediaType.TEXT_PLAIN);
            return;
        }

        protocol = getProjectProtocolService().getProtocolForProject(projectId, getUser());
        if (protocol == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Project " + projectId + " does not have a protocol associated with it.", MediaType.TEXT_PLAIN);
            return;
        }

        String subjectId = (String) getParameter(request, "SUBJECT_ID");
        if (subjectId != null) {
            subject = XnatSubjectdata.GetSubjectByProjectIdentifier(project.getId(), subjectId, getUser(), false);
        }
        if (subject == null) {
            subject = XnatSubjectdata.getXnatSubjectdatasById(subjectId, getUser(), false);
            if (subject != null && (project != null && !subject.hasProject(project.getId()))) {
                subject = null;
            }
        }
        if (subject == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Unable to identify subject " + subjectId, MediaType.TEXT_PLAIN);
        }

        String experimentId = (String) getParameter(request, "EXPERIMENT_ID");
        if (experimentId != null) {
            experiment = XnatExperimentdata.getXnatExperimentdatasById(experimentId, getUser(), completeDocument);
            if (experiment != null && (project != null && !experiment.hasProject(project.getId()))) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                response.setEntity("Experiment " + experimentId + " not associated with project " + projectId + ".", MediaType.TEXT_PLAIN);
                return;
            }
        }
        if (experiment == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Unable to identify experiment " + experimentId + ".", MediaType.TEXT_PLAIN);
            return;
        }

        String visitIdOrName = URLDecoder.decode((String) getParameter(request, "VISIT_ID_OR_NAME"), "UTF-8");
        if (visitIdOrName != null) {
            visit = XnatPvisitdata.getXnatPvisitdatasById(visitIdOrName, getUser(), completeDocument);
            //visits aren't really associated with projects in any way so we don't have to test that here.
        }
        // if no visit was found by id, we check to see whether we were passed the visit name instead
        if (visit == null) {
            CriteriaCollection cc = new CriteriaCollection("AND");
            cc.addClause("xnat:pVisitData/visit_name", visitIdOrName);
            cc.addClause("xnat:pVisitData/subject_id", subjectId);
            cc.addClause("xnat:experimentData/project", projectId);

            List<XnatPvisitdata> list = XnatPvisitdata.getXnatPvisitdatasByField(cc, getUser(), false);
            if (list.size() > 0) {
                visit = list.get(0);
            }
        }
        if (visit == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Unable to identify visit " + visitIdOrName + ".", MediaType.TEXT_PLAIN);
            return;
        }

        if (getQueryVariable("subtype") != null && !getQueryVariable("subtype").equals("undefined")) {
            subtype = URLDecoder.decode(getQueryVariable("subtype"), "UTF-8");
        }
        if (subtype == null || subtype.isEmpty()) {
            subtype = experiment.getProtocol();
        }

        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));
    }

    @Override
    public boolean allowDelete() {
        try {
            return experiment.canEdit(getUser());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handleDelete() {
        //this removes any association between the visit and experiment.
        boolean goterdone = false;
        String unsharedVisit = experiment.getVisit();
        String visitId = visit.getId();
        if (visit.getClosed() != null && visit.getClosed()) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("This visit has already been closed and may not have experiments removed from it.", MediaType.TEXT_PLAIN);
            return;
        }

        if (visitId.equalsIgnoreCase(unsharedVisit)) {
            experiment.setVisit("NULL");
            try {
                experiment.save(getUser(), true, false, null);
                goterdone = true;
            } catch (Exception e) {
                _log.error(e.getMessage());
                this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Unable to remove " + experiment.getId() + " from visit " + visit + ".", MediaType.TEXT_PLAIN);
                return;
            }
        } else {
            for (XnatExperimentdataShareI pp : experiment.getSharing_share()) {
                if (pp.getProject().equals(this.project.getId())) {
                    EventMetaI c = null;
                    try {
                        pp.setVisit("NULL");
                        SaveItemHelper.authorizedSave(((XnatExperimentdataShare) pp).getItem(), getUser(), true, false, c);
                        goterdone = true;
                    } catch (Exception e) {
                        _log.error(e.getMessage());
                        this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                        getResponse().setEntity("Unable to remove " + experiment.getId() + " from visit " + visit + ".", MediaType.TEXT_PLAIN);
                        return;
                    }
                    break;
                }
            }
        }
        if (goterdone) {
            this.getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Something didn't work... ", MediaType.TEXT_PLAIN);
        }
    }

    @Override
    public boolean allowPut() {
        try {
            return experiment.canEdit(getUser());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handlePut() {
        //this will associate an experiment with the visit if possible.
        String projectId = project.getId();
        String visitId = visit.getId();
        if (experiment.getVisit() != null && !experiment.getVisit().equals(visitId)) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("This experiment has already been associated with another visit. It must be unassociated first.", MediaType.TEXT_PLAIN);
            return;
        }
        if (visit.getClosed() != null && visit.getClosed()) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("This visit has already been closed and may not have additional experiments added to it.", MediaType.TEXT_PLAIN);
            return;
        }

        // we need to check if an experiment is unexpected if a) unexpected experiments are not allowed or b) they
        // are allowed but the user does not have permissions to assign them
        if (!protocol.getAllowUnexpectedExperiments() || (!UserHelper.getUserHelperService(getUser()).isOwner(projectId) && !Roles.isSiteAdmin(getUser()))) {

            // check whether experiment and protocol are on the list of expected experiments for this visit type
            try {
                // get info on the visit in question
                SubjectVisitInfo subjectVisitInfo = new SubjectVisitInfo(subject, projectId, getUser());
                SubjectVisitInfo.VisitInfo visitInfo = null;
                for (SubjectVisitInfo.VisitInfo vi : subjectVisitInfo.getVisits()) {
                    if (visitId.equals(vi.getId())) {
                        visitInfo = vi;
                    }
                }
                if (visitInfo == null) {
                    for (SubjectVisitInfo.VisitInfo vi : subjectVisitInfo.getUnexpectedVisits()) {
                        if (visitId.equals(vi.getId())) {
                            visitInfo = vi;
                        }
                    }
                }

                // see if there is at least one open slot for the type and subtype requested
                boolean unexpected = true;
                for (SubjectVisitInfo.ExperimentInfo experimentInfo : visitInfo.getExperiments()) {
                    // check that experiment type matches, that subtype matches or both are null, and that the slot is open
                    String expectedSubtype = experimentInfo.getSubtype() != null ? experimentInfo.getSubtype() : "";
                    String potentialSubtype = subtype != null ? subtype : "";
                    if (experimentInfo.getType().equals(experiment.getXSIType()) &&
                            ObjectUtils.equals(expectedSubtype, potentialSubtype) &&
                            experimentInfo.getExperiment() == null) {
                        unexpected = false;
                        break;
                    }
                }

                if (unexpected) {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                    if (!protocol.getAllowUnexpectedExperiments()) {
                        getResponse().setEntity("Unexpected experiment. Unexpected experiments may not be associated with visits under this protocol.", MediaType.TEXT_PLAIN);
                    }
                    else {
                        getResponse().setEntity("Unexpected experiment. Unexpected experiments may not be associated except by a project owner.", MediaType.TEXT_PLAIN);
                    }
                    return;
                }
            } catch (Exception e) {
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                getResponse().setEntity("The visit could not be checked to see if the experiment was unexpected.", MediaType.TEXT_PLAIN);
                return;
            }
        }

        if (projectId.equalsIgnoreCase(experiment.getProject())) {
            experiment.setVisit(visitId);
            if (subtype != null) {
                experiment.setProtocol(subtype);
            }
            try {
                experiment.save(getUser(), true, false, null);
            } catch (Exception e) {
                _log.error(e.getMessage());
                this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Unable to add visit " + visitId + " to experiment " + experiment.getId() + ".", MediaType.TEXT_PLAIN);
                return;
            }
        } else {
            for (XnatExperimentdataShareI pp : experiment.getSharing_share()) {
                if (pp.getProject().equals(this.project.getId())) {
                    experiment.setVisit(visitId);
                    if (subtype != null) {
                        experiment.setProtocol(subtype);
                    }
                    try {
                        experiment.save(getUser(), true, false, null);
                    } catch (Exception e) {
                        _log.error(e.getMessage());
                        this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                        getResponse().setEntity("Unable to add visit " + visitId + " to experiment " + experiment.getId() + ".", MediaType.TEXT_PLAIN);
                        return;
                    }
                }
            }
        }
        this.getResponse().setStatus(Status.SUCCESS_OK);
    }
}
