/*
 * org.nrg.xnat.restlet.extensions.ProjSubVisitList
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.restlet.extensions;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.util.ArrayList;
import java.util.List;

/*eerily similar to ProjSubExptList */

@XnatRestlet({"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits"})
public class ProjSubVisitList extends SecureResource {

    static Logger logger = Logger.getLogger(ProjSubVisitList.class);

    XnatProjectdata proj=null;
    XnatSubjectdata subject=null;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProjSubVisitList(Context context, Request request, Response response) {
        super(context, request, response);

        String pID = (String) request.getAttributes().get("PROJECT_ID");
        if(pID != null){
            proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
        }

        if (proj == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to identify project " + pID + ".", MediaType.TEXT_PLAIN);
            return;
        }

        String subID = (String) request.getAttributes().get("SUBJECT_ID");
        if(subID!=null){
            subject = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID, user, false);
            if(subject==null){
                subject = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);
                if (subject != null && (proj != null && !subject.hasProject(proj.getId()))) {
                    subject = null;
                }
            }
        }
        if(subject == null){
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Unable to identify subject " + subID + ".", MediaType.TEXT_PLAIN);
            return;
        }

        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));


        this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.VISIT_DATA,true));
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {

        try {
            SubjectVisitInfo subjectVisitInfo = null;
            subjectVisitInfo = new SubjectVisitInfo(subject, proj.getId(), user);

            if (this.isQueryVariableTrue("initial")) {
                List<String> initialVisits = new ArrayList<String>();
                List<VisitType> visitTypes = subjectVisitInfo.getProtocol().getVisitTypes();
                for (VisitType visitType : visitTypes) {
                    if (visitType.getInitial()) {
                        initialVisits.add(visitType.getName());
                    }
                }
                String response = mapper.writeValueAsString(initialVisits.toArray());
                return new StringRepresentation(response, MediaType.APPLICATION_JSON);
            }
            else if (this.isQueryVariableTrue("open")) {
                List<SubjectVisitInfo.VisitInfo> openVisits = new ArrayList<SubjectVisitInfo.VisitInfo>();
                for (SubjectVisitInfo.VisitInfo visitInfo : subjectVisitInfo.getVisits()) {
                    if (!visitInfo.getClosed()) {
                        openVisits.add(visitInfo);
                    }
                }
                String response = mapper.writeValueAsString(openVisits.toArray());
                return new StringRepresentation(response, MediaType.APPLICATION_JSON);
            }
            else if (this.getQueryVariable("sessionID") != null) {
                XnatExperimentdata session = XnatExperimentdata.getXnatExperimentdatasById(this.getQueryVariable("sessionID"), user, false);
                if (session != null) {
                    String type = session.getXSIType();
                    String subtype = this.getQueryVariable("subtype");
                    if (subtype == null) subtype = session.getProtocol();
                    String allVisits = mapper.writeValueAsString(subjectVisitInfo.getValidVisitsForExperiment(type, subtype));
                    return new StringRepresentation(allVisits, MediaType.APPLICATION_JSON);
                } else {
                    throw new Exception("");
                }
            }
            else {
                String allVisits = mapper.writeValueAsString(subjectVisitInfo.getVisits());
                return new StringRepresentation(allVisits, MediaType.APPLICATION_JSON);
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving visit list.");
        }
    }
}