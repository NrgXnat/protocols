/*
 * protocols: org.nrg.xnat.restlet.extensions.ProjectProtocolResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.extensions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XdatStoredSearch;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.util.ProtocolWrapper;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Map;


// validates a protocol (won't let you upload a bad one. If someone uploaded a bad one directly using the config 
// service, it won't let you download it.) handles get, put and deleteProtocol

@XnatRestlet({"/projects/{PROJECT_ID}/protocol","/projects/{PROJECT_ID}/protocol/{PROTOCOL_ID}"})
public class ProjectProtocolResource extends AbstractProtocolResource {

    private static final Log _log = LogFactory.getLog(ProjectProtocolResource.class);
    private final String projectId;
    private final Long protocolId;

    public ProjectProtocolResource(Context context, Request request, Response response) throws SQLException, DBPoolException, UnsupportedEncodingException {
        super(context, request, response);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        final Map<String, Object> attributes = request.getAttributes();
        projectId = attributes.containsKey("PROJECT_ID") ? (String) attributes.get("PROJECT_ID") : null;
        protocolId = attributes.containsKey("PROTOCOL_ID") ? Long.parseLong(URLDecoder.decode((String) attributes.get("PROTOCOL_ID"), "UTF-8")) : null;

        final boolean hasProjectID = !StringUtils.isBlank(projectId);
        if (!hasProjectID) {
            response.setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED);
            getResponse().setEntity("You must specify a project ID for this service call.", MediaType.TEXT_PLAIN);
            return;
        }

        _log.info(String.format("Found service call for project %s.", projectId));
    }

    @Override
    public boolean allowDelete() {
        return (Roles.isSiteAdmin(getUser()) || UserHelper.getUserHelperService(getUser()).isOwner(projectId));
    }

    @Override
    public void handleDelete() {
        if (!UserHelper.getUserHelperService(getUser()).isOwner(projectId) && !Roles.isSiteAdmin(getUser())) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            getResponse().setEntity("You do not have permissions to modify protocols associations with this project.", MediaType.TEXT_PLAIN);
            return;
        }
        getProjectProtocolService().removeProtocolFromProject(projectId, getUser());
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    @Override
    public boolean allowPut() {
        return (Roles.isSiteAdmin(getUser()) || UserHelper.getUserHelperService(getUser()).isOwner(projectId));
    }

    @Override
    public void handlePut() {
        XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectId, getUser(), false); // just checking to see if user has project access at all
        if (project == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("The project ID " + projectId + " does not result in a valid project.", MediaType.TEXT_PLAIN);
            return;
        }

        if (!UserHelper.getUserHelperService(getUser()).isOwner(projectId) && !Roles.isSiteAdmin(getUser())) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            getResponse().setEntity("You do not have permissions to modify protocols associations with this project.", MediaType.TEXT_PLAIN);
            return;
        }
        if (protocolId != null && protocolId != 0) {
            Protocol protocol = null;
            Integer version = null;
            try {
                version = new Integer(this.getQueryVariable("version"));
            }
            catch (Exception ignored) {} // almost certainly just a NumberFormatException

            if (version != null) {
                protocol = getProtocolService().getProtocolByIdAndVersion(protocolId, version);
            }
            else {
                protocol = getProtocolService().getProtocolById(protocolId);
            }
            getProjectProtocolService().assignProtocolToProject(project, protocol, getUser());
            getResponse().setStatus(Status.SUCCESS_OK);

            //DELETE storedSearches
            for(XdatStoredSearch bundle: project.getBundles()) {
                if(bundle.getBriefDescription() != null && bundle.getBriefDescription().startsWith("Visit:")){
                    try {
                        SaveItemHelper.authorizedDelete(bundle.getItem(), getUser(), EventUtils.newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, EventUtils.TYPE.WEB_SERVICE, "Saved Search Deletion"));
                    } catch (Throwable e) {
                        _log.error("",e);
                    }
                }
            }

            try {
                ProtocolWrapper wrapper = new ProtocolWrapper(protocol);
                wrapper.setMaxVersion(getProtocolService().getMaxProtocolVersion(protocolId));
                String protocolJson = mapper.writeValueAsString(wrapper);
                getResponse().setEntity(protocolJson, MediaType.APPLICATION_JSON);
            } catch (IOException ignored) {}
        }
        else {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            getResponse().setEntity("A protocol id is required.", MediaType.TEXT_PLAIN);

        }
    }


    @Override
    public Representation represent(Variant variant) throws ResourceException {
        XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectId, getUser(), false); // just checking to see if user has project access
        if (project == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "The project ID " + projectId + " does not result in a valid project.");
        }

        Protocol protocol = null;
        Integer version = null;
        try {
            version = new Integer(this.getQueryVariable("version"));
        }
        catch (Exception ignored) {} // almost certainly just a NumberFormatException

        if (version != null) {
            protocol = getProjectProtocolService().getPreviousProtocolForProject(projectId, version, getUser());
        }
        else {
            protocol = getProjectProtocolService().getProtocolForProject(projectId, getUser());
        }

        if (protocol == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "The project ID " + projectId + " does not have a protocol associated with it.");
        }

        try {
            ProtocolWrapper wrapper = new ProtocolWrapper(protocol);
            wrapper.setMaxVersion(getProtocolService().getMaxProtocolVersion(protocol.getProtocolId()));
            String protocolJson = mapper.writeValueAsString(wrapper);
            return new StringRepresentation(protocolJson, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            _log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving project protocol information.");
        }
    }
}
