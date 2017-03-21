/*
 * protocols: org.nrg.xnat.restlet.extensions.VisitResource 
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.extensions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.EventRequirementAbsent;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.util.ProtocolVisitSubjectHelper;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.utils.WorkflowUtils;
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
import java.util.Collection;
import java.util.List;

@XnatRestlet({"/visits/{VISIT_ID}", "/projects/{PROJECT_ID}/visits/{VISIT_ID}", "/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits/{VISIT_ID}"})
public class VisitResource extends AbstractProtocolResource{

    private static final Log _log = LogFactory.getLog(VisitResource.class);
    private final ObjectMapper mapper = new ObjectMapper();
    XnatProjectdata project = null;
    XnatSubjectdata subject = null;
	XnatPvisitdata visit = null;
    Protocol protocol = null;
    private ProtocolVisitSubjectHelper protocolHelper = new ProtocolVisitSubjectHelper();
	
	public VisitResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		//validate the project ID if one was passed in.                           ...close visit hits here
		String pID = (String)request.getAttributes().get("PROJECT_ID");
		if(pID!=null){
			project = XnatProjectdata.getProjectByIDorAlias(pID, getUser(), false);
			if(project == null){
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Unable to identify project " + pID + ".", MediaType.TEXT_PLAIN);
                return;
			}
		}

        String subID= (String)request.getAttributes().get("SUBJECT_ID");
        if(subID!=null){
            subject = XnatSubjectdata.GetSubjectByProjectIdentifier(project.getId(), subID, getUser(), false);

            if(subject==null){
                subject = XnatSubjectdata.getXnatSubjectdatasById(subID, getUser(), false);
                if (subject != null && (project != null && !subject.hasProject(project.getId()))) {
                    subject = null;
                }
            }
        }

		//let's try to find the visit. If a project was passed in, let's also make sure the visit is a member of that project.
		String visitID= (String)request.getAttributes().get("VISIT_ID");		
		if(visitID!=null){
			visit=XnatPvisitdata.getXnatPvisitdatasById(visitID, getUser(), false);
            if (visit == null && project != null) {
                visit = XnatPvisitdata.GetVisitByProjectIdentifier(project.getId(), visitID, getUser(), false);
            }
			if(project !=null && visit!=null) {
				//make sure the visit has the passed in project
				if(!visit.hasProject(project.getId())){
					response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                    getResponse().setEntity("Visit does not belong to project " + project.getId() + ".", MediaType.TEXT_PLAIN);
                }
			}
            else if (visit!=null) {
                project = XnatProjectdata.getXnatProjectdatasById(visit.getProject(), getUser(), false);
            }
            if(subject !=null && visit!=null) {
                //make sure the visit matches the subject
                if(!subject.getId().equals(visit.getSubjectId())){
                    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                    getResponse().setEntity("Visit does not belong to subject " + subject.getId() + ".", MediaType.TEXT_PLAIN);
                }
            }
            else if (visit!=null) {
                subject = XnatSubjectdata.getXnatSubjectdatasById(visit.getSubjectId(), getUser(), false);
            }
		}

        if (project != null) {
            protocol = getProjectProtocolService().getProtocolForProject(project.getId(), getUser());
        }
        if (protocol == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            getResponse().setEntity("Protocol not found.", MediaType.TEXT_PLAIN);
            return;
        }
		
		if(visit!=null){			
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}	
	
	@Override
	public boolean allowDelete() {
		return true;
	}
	@Override
	public void handleDelete(){
		try {
			if(project == null){
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                getResponse().setEntity("Unable to identify project. Please use the project/visit URI.", MediaType.TEXT_PLAIN);
            }
			
			if(visit!=null){
				PersistentWorkflowI wrk;
				try {
					wrk = WorkflowUtils.buildOpenWorkflow(getUser(), visit.getItem(),newEventInstance(EventUtils.CATEGORY.DATA,(getAction()!=null)?getAction():EventUtils.getDeleteAction(visit.getXSIType())));
					EventMetaI c=wrk.buildEvent();
					
					try {
						String msg=visit.delete(project, getUser(), this.isQueryVariableTrue("removeFiles"),c);
						if(msg!=null){
							WorkflowUtils.fail(wrk, c);
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                            getResponse().setEntity(msg, MediaType.TEXT_PLAIN);
                        }else{
							WorkflowUtils.complete(wrk, c);
						}
					} catch (Exception e) {
						try {
							WorkflowUtils.fail(wrk, c);
						} catch (Exception e1) {
							logger.error("",e1);
						}
						logger.error("",e);
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                        getResponse().setEntity("Error deleting visit.", MediaType.TEXT_PLAIN);
                    }
				} catch (EventRequirementAbsent e1) {
					logger.error("",e1);
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                    getResponse().setEntity("Error deleting visit.", MediaType.TEXT_PLAIN);
                }
			}
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity("Error deleting visit.", MediaType.TEXT_PLAIN);
            logger.error("",e);
		}
	}
	
	@Override
	public boolean allowPost() {
		return true;
	}
	@Override
	public void handlePost(){
        if (!UserHelper.getUserHelperService(getUser()).isOwner(project.getId()) && !Roles.isSiteAdmin(getUser())) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            getResponse().setEntity("Specified user account has insufficient privileges for subjects in this project.", MediaType.TEXT_PLAIN);
            return;
        }

		if(isQueryVariableTrueHelper(this.getQueryVariable("close"))){
            try {
                // SubjectVisitInfo already has a built-in visit requirements analyzer, so we'll use that
                boolean closeable = false;
                SubjectVisitInfo subjectVisitInfo = new SubjectVisitInfo(subject, project.getId(), getUser());
                for (SubjectVisitInfo.VisitInfo visitInfo : subjectVisitInfo.getVisits()) {
                    if (visitInfo.getId().equals(visit.getId())) {
                        closeable = visitInfo.getRequirementsFilled();
                    }
                }
                for (SubjectVisitInfo.VisitInfo visitInfo : subjectVisitInfo.getUnexpectedVisits()) {
                    if (visitInfo.getId().equals(visit.getId())) {
                        // unexpected visits have no protocol information and thus are always closeable
                        closeable = true;
                    }
                }

                if (closeable) {
                    visit.setClosed(true);
                    visit.setProtocolid(protocol.getProtocolId().toString());
                    visit.setProtocolversion(protocol.getVersion());
                    visit.save(getUser(), true, false, null);
                    this.getResponse().setStatus(Status.SUCCESS_OK);
                    return;
                }
                else {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED);
                    if (protocol.getAllowUnexpectedExperiments()) {
                        this.getResponse().setEntity("Visit is missing required experiments.", MediaType.TEXT_PLAIN);
                    }
                    else {
                        this.getResponse().setEntity("Visit is missing required experiments or has an unexpected experiment associated to it.", MediaType.TEXT_PLAIN);
                    }
                    return;
                }
			} catch (Exception e) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                getResponse().setEntity("Unable to close visit " + visit.getId() + ".", MediaType.TEXT_PLAIN);
				return;
			}
		}
		if(isQueryVariableTrueHelper(this.getQueryVariable("open"))){
            if (!protocol.getAllowMultipleOpenVisits()) {
                CriteriaCollection cc = new CriteriaCollection("AND");
                cc.addClause("xnat:pVisitData/subject_id", visit.getSubjectId());
                cc.addClause("xnat:experimentData/project", project.getId());
                cc.addClause("xnat:pVisitData/closed", false);

                List<XnatPvisitdata> list = XnatPvisitdata.getXnatPvisitdatasByField(cc, getUser(), false);
                if (list.size() > 0) {
                    this.getResponse().setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED);
                    this.getResponse().setEntity("Under project protocol, multiple visits may not be open at the same time.", MediaType.TEXT_PLAIN);
                    return;
                }
            }

			visit.setClosed(false);
			try {
				visit.save(getUser(), true, false, null);
				this.getResponse().setStatus(Status.SUCCESS_OK);
			} catch (Exception e) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                getResponse().setEntity("Unable to open visit " + visit.getId() + ".", MediaType.TEXT_PLAIN);
            }
		}
	}

    @Override
    public Representation represent(Variant variant) throws ResourceException {
		if(visit!=null){
            try {
                if (this.isQueryVariableTrue("subtypes")) {
                    Collection<String> subtypes = protocolHelper.getSubtypesForVisit(visit.getId(), getUser());
                    String response = mapper.writeValueAsString(subtypes);
                    return new StringRepresentation(response, MediaType.APPLICATION_JSON);
                }
                else {
                    return this.representItem(visit.getItem(), overrideVariant(variant));
                }
            } catch (IOException e) {
                _log.error(e);
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error retrieving visit information.");
            }
		}else{
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified visit.");
		}
	}
}
