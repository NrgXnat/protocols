package org.nrg.xnat.restlet.extensions;/*
 * org.nrg.xnat.restlet.extensions.ProtocolResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 8/16/13 11:05 AM
 * Author: Justin Cleveland (clevelandj@mir.wustl.edu)
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.util.FindVisitsNearSchedulingOrExceptionJob;
import org.nrg.xnat.protocol.util.VisitReportInfo;
import org.nrg.xnat.restlet.XnatRestlet;
import org.quartz.JobExecutionException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@XnatRestlet({"/projects/{PROJECT_ID}/visit/report"})
public class VisitReportResource extends AbstractProtocolResource {
    private static final Log _log = LogFactory.getLog(VisitReportResource.class);
    private String projectId;
    private boolean sendNotificationEmail = false;

    public VisitReportResource(Context context, Request request, Response response) throws UnsupportedEncodingException {
        super(context, request, response);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        final Map<String, Object> attributes = request.getAttributes();
        projectId = attributes.containsKey("PROJECT_ID") ? (String) attributes.get("PROJECT_ID") : null;

        final boolean hasProjectID = !StringUtils.isBlank(projectId);
        if (!hasProjectID) {
            response.setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED);
            getResponse().setEntity("You must specify a project ID for this service call.", MediaType.TEXT_PLAIN);
            return;
        }
        _log.info(String.format("Found service call for project %s.", projectId));
        if(isQueryVariableTrueHelper(this.getQueryVariable("sendNotificationEmail"))){
            sendNotificationEmail = true;
        }
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return false;
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        String response = "{}";
        if (projectId != null) { // get visit information on a specific project
            XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectId, user, false); // just checking to see if user has project access
            if (project == null) {
                return new StringRepresentation("{\"error\":\"The project ID "+projectId+" does not exist or you do not have access to it.\"}", MediaType.APPLICATION_JSON);
            }
            Protocol protocol = _projectProtocolService.getProtocolForProject(projectId, user);
            if (protocol == null) {
                return new StringRepresentation("{\"error\":\"No protocol is associated with project ID: "+projectId+"\"}", MediaType.APPLICATION_JSON);
            }
            if(sendNotificationEmail){
                System.out.println("Sending Visit Report Notification Email...");
                try {
                    _findVisitsNearSchedulingOrExceptionJob.execute();
                } catch (JobExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    HashMap vr = getVisitReport(project, protocol);
                    response = mapper.writeValueAsString(vr);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        StringRepresentation sr = new StringRepresentation(response, MediaType.APPLICATION_JSON);
        return sr;
    }

    public static HashMap <String, VisitReportInfo> getVisitReport(XnatProjectdata project, Protocol protocol) {
        List <VisitType> visitTypes = protocol.getVisitTypes();
        HashMap <String, VisitReportInfo> visitReport = new HashMap();
        // create separate buckets for open, closed, upcoming, etc...
        if(visitTypes != null) {
            int i=0;
            VisitType baselineVisit = null;
            for (final VisitType visitType : visitTypes) {
                if (i == 0) {
                    baselineVisit = visitType;
                } else if (baselineVisit != null){
                    int deltaNextOpen = visitType.getDelta() - visitType.getDeltaLow();
                    int deltaNextClose = visitType.getDelta() + visitType.getDeltaHigh();
                    if (visitType.getName() != null && project.getId() != null) {
                        try {
                            // This is only partially the query still under development...
                            String query = "SELECT new_visit.subject_id as subject, next_open, next_closed, COALESCE (visit_1.id, " +
                                    "CASE " +
                                    "WHEN (now() > next_open and now() < next_closed) then 'open' " +
                                    "WHEN (now() > next_closed) then 'absent' " +
                                    "WHEN (now() > (next_open - interval '30 days')) then 'upcoming' " +
                                    "ELSE 'N/A' " + // ...too far in the future
                                    "END) as status " +
                                    "FROM ( " +
                                    "SELECT pv.id, expt.date, pv.subject_id, expt.date+interval '" + deltaNextOpen + " days' as next_open, expt.date+interval '" + deltaNextClose + " days' as next_closed, expt.project, pv.protocolid, delta, delta_high, delta_low " +
                                    "FROM xnat_pvisitdata pv " +
                                    "LEFT JOIN xhbm_protocol proc on pv.id = cast(proc.id as text) " +
                                    "LEFT JOIN xhbm_visit_type xvt on proc.id = xvt.protocol AND xvt.name = '"+baselineVisit.getName()+"' " +
                                    "LEFT JOIN xnat_experimentdata expt on pv.id = expt.id " +
                                    "WHERE expt.project='" + project.getId() + "' AND protocolid = '" + protocol.getProtocolId() + "'" +
                                    ") new_visit " +
                                    "LEFT JOIN ( " +
                                    "SELECT pv.id, subject_id, expt.project, pv.protocolid " +
                                    "FROM xnat_pvisitdata pv " +
                                    "LEFT JOIN xnat_experimentdata expt on pv.id = expt.id " +
                                    "WHERE visit_type = '" + visitType.getName() + "' " +
                                    ") visit_1 on new_visit.subject_id = visit_1.subject_id AND new_visit.project = visit_1.project AND new_visit.protocolid = visit_1.protocolid";
                            XFTTable table = XFTTable.Execute(query, null, null);
                            table.resetRowCursor();
                            while (table.hasMoreRows()) {
                                final Hashtable row = table.nextRowHash();
                                VisitReportInfo vri = new VisitReportInfo();
                                vri.setSubjectId((String) row.get("subject"));
                                Timestamp ts = (Timestamp) row.get("next_open");
                                if(ts != null) {
                                    vri.setNextOpen(new Date(ts.getTime()));
                                }
                                ts = (Timestamp) row.get("next_closed");
                                if(ts != null) {
                                    vri.setNextClosed(new Date(ts.getTime()));
                                }
                                vri.setStatus((String)row.get("status"));
                                visitReport.put(vri.getSubjectId(), vri);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (DBPoolException e) {
                            e.printStackTrace();
                        }
                    }
                }
                i++;
            }
        }
        return visitReport;
    }

    @Inject
    private FindVisitsNearSchedulingOrExceptionJob _findVisitsNearSchedulingOrExceptionJob;
}
