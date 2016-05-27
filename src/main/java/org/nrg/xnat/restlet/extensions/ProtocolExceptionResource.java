package org.nrg.xnat.restlet.extensions;/*
 * org.nrg.xnat.restlet.extensions.ProtocolExceptionResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/27/13 11:51 AM
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.ProtocolException;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

@XnatRestlet({"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits/{VISIT_ID}/exceptions"})
public class ProtocolExceptionResource extends AbstractProtocolResource {

    private static final Log _log = LogFactory.getLog(ProtocolExceptionResource.class);
    XnatProjectdata project = null;
    XnatSubjectdata subject = null;
    XnatPvisitdata visit = null;
    Protocol protocol;
    String xsiType;
    String subtype;
    String reason;
    String explanation;

    public ProtocolExceptionResource(Context context, Request request, Response response) {
        super(context, request, response);

        String projectId = (String) getParameter(request, "PROJECT_ID");
        if (projectId != null) {
            project = XnatProjectdata.getProjectByIDorAlias(projectId, user, false);
        }
        if (project == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to identify project " + projectId + ".", MediaType.TEXT_PLAIN);
            return;
        }

        protocol = getProjectProtocolService().getProtocolForProject(projectId, user);
        if (protocol == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Project " + projectId + " does not have a protocol associated with it.", MediaType.TEXT_PLAIN);
            return;
        }

        String subjectId = (String) getParameter(request, "SUBJECT_ID");
        if (subjectId != null) {
            subject = XnatSubjectdata.GetSubjectByProjectIdentifier(project.getId(), subjectId, user, false);
        }
        if (subject == null) {
            subject = XnatSubjectdata.getXnatSubjectdatasById(subjectId, user, false);
            if (subject != null && (project != null && !subject.hasProject(project.getId()))) {
                subject = null;
            }
        }
        if (subject == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to identify subject " + subject + ".", MediaType.TEXT_PLAIN);
        }

        String visitId = (String) getParameter(request, "VISIT_ID");
        if (visitId != null) {
            visit = XnatPvisitdata.getXnatPvisitdatasById(visitId, user, completeDocument);
            //visits aren't really associated with projects in anyway so we don't have to test that here.
        }
        if (visit == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to identify visit " + visitId + ".", MediaType.TEXT_PLAIN);
            return;
        }

        xsiType = getQueryVariable("xsiType");
        if (xsiType == null) xsiType = "";

        subtype = getQueryVariable("subtype");
        if (StringUtils.isNotEmpty(subtype)) {
            subtype = subtype.replace("&amp;", "&");
        }
        reason = getQueryVariable("reason");
        if (reason == null) reason = "Other";
        explanation = getQueryVariable("explanation");
        if (StringUtils.isNotEmpty(explanation)) {
            explanation = explanation.replace("&amp;", "&");
        }
        if (explanation != null && explanation.length() > 255) explanation = explanation.substring(0, 254);
    }

    @Override
    public boolean allowPost() {
        return (Roles.isSiteAdmin(user) || UserHelper.getUserHelperService(user).isOwner(project.getId()));
    }

    @Override
    public void handlePost() {
        if (StringUtils.isEmpty(xsiType)) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("The type of experiment to be exempted must be included in the request.", MediaType.TEXT_PLAIN);
            return;
        }

        if (project == null || subject == null || visit == null) {
            return; // messages for each of these cases set in the constructor
        }
        if (visit.getClosed()) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            getResponse().setEntity("Deviations may not be created for this visit as it is already closed.", MediaType.TEXT_PLAIN);
            return;
        }

        ProtocolException protocolException = new ProtocolException(reason, project.getId(), subject.getId(), visit.getId(), xsiType, user.getUsername());
        protocolException.set_date(new Date(new java.util.Date().getTime()));
        if (subtype != null) {
            protocolException.set_subtype(subtype);
        }
        if (explanation != null) {
            protocolException.set_explanation(explanation);
        }

        getProtocolExceptionService().create(protocolException);
    }

    @Override
    public boolean allowPut() {
        return allowPost();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void handlePut() {
        this.handlePost();
    }

    @Override
    public boolean allowDelete() {
        return (Roles.isSiteAdmin(user) || UserHelper.getUserHelperService(user).isOwner(project.getId()));
    }

    @Override
    public void handleDelete() {
        if (StringUtils.isEmpty(xsiType)) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("The type of deviation to be deleted must be included in the request.", MediaType.TEXT_PLAIN);
        }
        else {
            if (visit.getClosed()) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                getResponse().setEntity("Deviations may not be deleted from this visit as it is already closed.", MediaType.TEXT_PLAIN);
                return;
            }

            ProtocolException ex = getProtocolExceptionService().findExceptionForVisitAndType(visit.getId(), xsiType, subtype);
            if (ex != null) {
                getProtocolExceptionService().delete(ex);
                getResponse().setStatus(Status.SUCCESS_OK);
            }
            else {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Could not find a unique deviation of specified type. You may need to include a subtype.", MediaType.TEXT_PLAIN);
            }
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        try {
            if (StringUtils.isEmpty(xsiType)) {
                List<ProtocolException> list = getProtocolExceptionService().findExceptionsForVisit(visit.getId());
                String listJSON = mapper.writeValueAsString(list);
                return new StringRepresentation(listJSON, MediaType.APPLICATION_JSON);
            }
            else {
                ProtocolException ex = getProtocolExceptionService().findExceptionForVisitAndType(visit.getId(), xsiType, subtype);
                if (ex != null) {
                    String exceptionJSON = mapper.writeValueAsString(ex);
                    return new StringRepresentation(exceptionJSON, MediaType.APPLICATION_JSON);
                }
                else {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not find a unique deviation of specified type in this visit. You may need to include a subtype.");
                }
            }
        } catch (IOException e) {
            _log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving protocol deviation information.");
        }
    }
}
