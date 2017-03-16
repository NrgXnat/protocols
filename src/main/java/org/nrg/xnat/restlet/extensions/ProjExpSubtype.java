package org.nrg.xnat.restlet.extensions;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 10/2/13 9:27 AM
 */

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatPvisitdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@XnatRestlet({"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPERIMENT_ID}/subtype", "/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPERIMENT_ID}/subtype/{SUBTYPE}"})
public class ProjExpSubtype extends AbstractProtocolResource {
    private static final Log _log = LogFactory.getLog(ProjExpSubtype.class);
    XnatProjectdata project = null;
    XnatSubjectdata subject = null;
    XnatExperimentdata experiment = null;
    Protocol protocol;
    String subtype = null;

    public ProjExpSubtype(Context context, Request request, Response response) throws UnsupportedEncodingException {
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

        subtype = request.getAttributes().containsKey("SUBTYPE") ? URLDecoder.decode((String) getParameter(request, "SUBTYPE"), "UTF-8") : null;
        // expanded to allow subtypes to be entered as query variables to get around a Tomcat problem with encoded slashes in parameters
        if (subtype == null) {
            subtype = this.getQueryVariable("subtype").replace("&amp;", "&"); // subtype will have been double encoded
        }

        if (subtype != null && subtype.equals("undefined")) {
            subtype = null;
        }
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
        experiment.setProtocol("NULL");
        try {
            experiment.save(getUser(), true, false, null);
        } catch (Exception e) {
            _log.error(e.getMessage());
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to remove subtype from experiment " + experiment.getId() + ".", MediaType.TEXT_PLAIN);
            return;
        }
        getResponse().setStatus(Status.SUCCESS_OK);
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
        String projectId = project.getId();
        String visitId = experiment.getVisit();
        XnatPvisitdata visit = AutoXnatPvisitdata.getXnatPvisitdatasById(visitId, getUser(), false);

        if (visit != null) {
            if (visit.getClosed() != null && visit.getClosed()) {
                this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("This visit has already been closed and may not have additional experiments added to it.", MediaType.TEXT_PLAIN);
                return;
            }

            // we need to check if an experiment is unexpected if a) unexpected experiments are not allowed or b) they
            // are allowed but the user does not have permissions to assign them
            if (!protocol.getAllowUnexpectedExperiments() || (!UserHelper.getUserHelperService(getUser()).isOwner(subject.getProject()) && !Roles.isSiteAdmin(getUser()))) {

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
                        if (experimentInfo.getType().equals(experiment.getXSIType()) &&
                                ObjectUtils.equals(experimentInfo.getSubtype(), subtype) &&
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
        }

        if (subtype == null || subtype == "") {
            this.handleDelete();
        }
        else {
            experiment.setProtocol(subtype);
            try {
                experiment.save(getUser(), true, false, null);
            } catch (Exception e) {
                _log.error(e.getMessage());
                this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Unable to set subtype for experiment " + experiment.getId() + ".", MediaType.TEXT_PLAIN);
                return;
            }
            getResponse().setStatus(Status.SUCCESS_OK);
        }
    }
}
