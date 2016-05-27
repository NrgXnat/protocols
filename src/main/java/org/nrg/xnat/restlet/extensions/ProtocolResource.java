package org.nrg.xnat.restlet.extensions;/*
 * org.nrg.xnat.restlet.extensions.ProtocolResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/16/13 11:05 AM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.ProtocolLineage;
import org.nrg.xnat.protocol.util.ProtocolWrapper;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.springframework.security.core.GrantedAuthority;

import java.io.*;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@XnatRestlet({"/protocol", "/protocol/{PROTOCOL_ID}"})
public class ProtocolResource extends AbstractProtocolResource {

    private static final Log _log = LogFactory.getLog(ProtocolResource.class);
    private Long protocolId;

    public ProtocolResource(Context context, Request request, Response response) throws UnsupportedEncodingException {
        super(context, request, response);

        setModifiable(true);
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        final Map<String, Object> attributes = request.getAttributes();
        protocolId = attributes.containsKey("PROTOCOL_ID") ? Long.parseLong(URLDecoder.decode((String) attributes.get("PROTOCOL_ID"), "UTF-8")) : null;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleDelete() {
        // check to make sure user is on the white list for the protocol
        Protocol protocol = _protocolService.getProtocolById(protocolId);
        if (protocol == null || (!Roles.isSiteAdmin(user) && !protocol.getUserWhiteList().contains(user.getUsername()))) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            return;
        }
        _protocolService.deleteProtocol(protocolId);
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        try {
            FileWriterWrapperI fw;
            List<FileWriterWrapperI> fws = this.getFileWriters();
            if (fws.size() == 0) {
                this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                getResponse().setEntity("Unable to identify upload format.", MediaType.TEXT_PLAIN);
                return;
            }

            if (fws.size() > 1) {
                this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                getResponse().setEntity("Importer is limited to one uploaded resource at a time.", MediaType.TEXT_PLAIN);
                return;
            }
            fw = fws.get(0);

            //read the input stream into a string buffer.
            final InputStream is = fw.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();

            String contents = sb.toString();

            //validate the contents (the protocol string)
            Protocol theProtocol;
            Protocol existing = null;
            try {
                theProtocol = mapper.readValue(contents, Protocol.class);

                //validate the incoming protocol
                String validateString = theProtocol.validate();
                if (validateString != null) {
                    _log.error("the protocol is invalid: " + validateString);
                    this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    getResponse().setEntity("The submitted protocol cannot be parsed:\n" + validateString, MediaType.TEXT_PLAIN);
                    return;
                }
                if (!theProtocol.getUserWhiteList().contains(user.getUsername())) {
                    theProtocol.getUserWhiteList().add(user.getUsername());
                }

                ProtocolLineage theLineage = null;
                if (protocolId != null) {
                    existing = _protocolService.getProtocolById(protocolId);
                    if (existing != null) {
                        theLineage = existing.getProtocolLineage();
                    }
                }
                //protocol part of an existing lineage
                if (theLineage != null) {
                    if(!Roles.isSiteAdmin(user) && !theLineage.getUserWhiteList().contains(user.getUsername())) {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                        getResponse().setEntity("You are attempting to modify a protocol to which you do not have access.", MediaType.TEXT_PLAIN);
                        return;
                    }

                    // check to see if the lineage needs to be updated
                    if (!theLineage.equals(theProtocol.getProtocolLineage())) {
                        theLineage.setUserWhiteList(theProtocol.getUserWhiteList());
                        _protocolLineageService.update(theLineage);
                    }
                    if (!theProtocol.equals(existing)) {
                        theProtocol.setProtocolLineage(theLineage);
                        theProtocol.setVersion(theLineage.getProtocols().size() + 1);
                        theProtocol.cleanChildIds();
                        theProtocol = _protocolService.storeProtocol(theProtocol, user);
                    }
                }
                //protocol part of a new lineage
                else {
                    // only users who are owners of at least one project should be able to create protocols
                    Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) user.getAuthorities();
                    boolean allowed =  Roles.isSiteAdmin(user); // site admins and owners of at least one project are allowed to create protocols
                    for (GrantedAuthority authority : authorities) {
                        if (allowed || authority.getAuthority().endsWith("_owner")) {
                            allowed = true;
                            break;
                        }
                    }

                    if (!allowed) {
                        this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                        getResponse().setEntity("You must be an owner on at least one project to create a protocol.", MediaType.TEXT_PLAIN);
                        return;
                    }

                    theLineage = new ProtocolLineage();
                    theLineage.setUserWhiteList(theProtocol.getUserWhiteList());
                    _protocolLineageService.create(theLineage);

                    theProtocol.setProtocolLineage(theLineage);
                    theProtocol.setVersion(1);
                    theProtocol = _protocolService.storeProtocol(theProtocol, user);
                }
                ProtocolWrapper wrapper = new ProtocolWrapper(theProtocol);
                wrapper.setMaxVersion(theProtocol.getVersion());
                String protocolJson = mapper.writeValueAsString(wrapper);

                getResponse().setStatus(Status.SUCCESS_CREATED);
                getResponse().setEntity(protocolJson, MediaType.APPLICATION_JSON);
            } catch (Exception e) {
                this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                getResponse().setEntity("The submitted protocol cannot be parsed: \n" + e.toString(), MediaType.TEXT_PLAIN);
            }
        } catch (Exception e) {
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity("Error encountered while attempting to upload or parse protocol.", MediaType.TEXT_PLAIN);
            _log.error(e);
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (protocolId != null) { // get information on a specific protocol
            // the 'projectsUsing' flag should be set when retrieving the list of projects currently using some version of this protocol
            if (isQueryVariableTrueHelper(this.getQueryVariable("projectsUsing"))) {
                // check to make sure user is on the white list for the protocol
                Protocol protocol = _protocolService.getProtocolById(protocolId);
                if (protocol == null || (!Roles.isSiteAdmin(user) && !protocol.getUserWhiteList().contains(user.getUsername()))) {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not locate protocol with id " + protocolId +".");
                }
                try {
                    List<ProjectProtocol> projects = _protocolService.getProjectsUsingProtocol(protocolId, user);
                    Collections.sort(projects);
                    String projectList = mapper.writeValueAsString(projects);
                    return new StringRepresentation(projectList, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    _log.error(e);
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving protocol information.");
                }
            }
            else {
                // otherwise, just do a basic GET on the protocol
                Protocol protocol;
                Integer version = null;
                try {
                    version = new Integer(this.getQueryVariable("version"));
                }
                catch (Exception ignored) {} // almost certainly just a NumberFormatException

                if (version != null) {
                    protocol = _protocolService.getProtocolByIdAndVersion(protocolId, version);
                }
                else {
                    protocol = _protocolService.getProtocolById(protocolId);
                }

                if (protocol != null) {
                    try {
                        if (!Roles.isSiteAdmin(user) && !protocol.getUserWhiteList().contains(user.getUsername())) {
                            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not locate protocol with id " + protocolId +".");
                        }
                        ProtocolWrapper wrapper = new ProtocolWrapper(protocol);
                        wrapper.setMaxVersion(_protocolService.getMaxProtocolVersion(protocolId));
                        String forExport = this.getQueryVariable("export");
                        if("true".equals(forExport)){
                            // Remove the protocol ids to prevent re-importing this protocol over top of an existing one later
                            protocol.setId(0);
                            //protocol.setProtocolId(new Long(0));
                            protocol.cleanChildIds();
                        }
                        String protocolJson = mapper.writeValueAsString(wrapper);

                        setResponseHeader("Content-Disposition", "attachment; filename=" + protocol.getName().replaceAll("\\W", "_") + ".json");
                        return new StringRepresentation(protocolJson, MediaType.APPLICATION_JSON);
                    } catch (IOException e) {
                        _log.error(e);
                        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving protocol information.");
                    }
                } else {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not locate protocol with id " + protocolId +".");
                }
            }
        }
        else { // get a list of all protocols the user is white-listed on
            try {
                List<Protocol> list = _protocolService.getAvailableProtocols(user);
                String protocols = mapper.writeValueAsString(list.toArray());
                return new StringRepresentation(protocols, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                _log.error(e);
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving protocol information.");
            }
        }
    }
}
