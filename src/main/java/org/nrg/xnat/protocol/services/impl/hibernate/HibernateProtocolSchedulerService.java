/*
 * protocols: org.nrg.xnat.protocol.services.impl.hibernate.HibernateProtocolSchedulerService
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.services.impl.hibernate;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.ProjectProtocol;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.services.ProjectProtocolService;
import org.nrg.xnat.protocol.services.ProtocolSchedulerService;
import org.nrg.xnat.protocol.util.VisitReportInfo;
import org.nrg.xnat.restlet.extensions.VisitReportResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class HibernateProtocolSchedulerService implements ProtocolSchedulerService {

    /**
     * This method finds any visits for the specified projects that are within the window specified by the {@link
     * ProjectProtocol project protocol} and have not yet been scheduled. If no project IDs are specified, then all
     * projects with associated protocols are searched.
     *
     * @param projectIds Zero or more project IDs for the projects to be searched for visits nearing the scheduling
     *                   window.
     * @return A list of all visits nearing the scheduling window.
     */
    @Transactional
    @Override
    public List<XnatPvisitdata> findVisitsNearScheduling(final UserI user, final String... projectIds) {
        final Map<String, Protocol> protocols = getProtocolsForProjects(user, projectIds);
        ArrayList<XnatPvisitdata> pVisits = new ArrayList<XnatPvisitdata>();

        if (_log.isDebugEnabled()) {
            _log.debug("Found " + protocols.size() + " projects with associated protocols.");
        }

        Map<String, Protocol> projectProtocols = getProtocolsForProjects(user, projectIds);
        for (Map.Entry<String, Protocol> projectProtocolMap : projectProtocols.entrySet()){
            XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectProtocolMap.getKey(), (XDATUser) user, false);
            Protocol projectProtocol = projectProtocolMap.getValue();
            final Map <String, VisitReportInfo> visitReports = VisitReportResource.getVisitReport(project, projectProtocol);
            if(projectProtocol.getEmailNotifications().contains("visitApproachingWindow")) {
                //Only send notification emails if requested
                for (Map.Entry<String, VisitReportInfo> visitReport : visitReports.entrySet()) {
                    VisitReportInfo visitReportInfo = visitReport.getValue();
                    Date nextOpen = visitReportInfo.getNextOpen();
                    Date nextClosed = visitReportInfo.getNextClosed();
//                    Date currentDate = new Date();
                    //if (currentDate.compareTo(nextOpen) > 0 && !(currentDate.compareTo(nextClosed) > 0)) {
                        //Send an email if visit has not yet been scheduled, and should be scheduled for a date in a period starting in less than a month.
                    if (visitReportInfo.getStatus().equals("upcoming")) {
                        //Send emails to these addresses: projectProtocol.getValue().getDefaultNotificationEmails()
                        for (String emailAddress : projectProtocol.getDefaultNotificationEmails()) {
                            String formattedOpenDate = new SimpleDateFormat("MMMM d, yyyy").format(nextOpen);
                            String formattedClosedDate = new SimpleDateFormat("MMMM d, yyyy").format(nextClosed);
                            String body = "A visit needs to be scheduled for subject " + visitReportInfo.getSubjectId() + " as part of project " + project.getDisplayName() + ". This visit should be scheduled for a date between " + formattedOpenDate + " and " + formattedClosedDate + ".";

                            String subject = "Visit Needs to be Scheduled for " + project.getDisplayName();
                            sendEmail(emailAddress, subject, body);
                        }
                    }
                    //}
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * This method finds any visits for the specified projects that are nearing the allowable delta drift specified by
     * the {@link ProjectProtocol project protocol} and have not yet been complete. If no project IDs are specified,
     * then all projects with associated protocols are searched.
     *
     * @param projectIds Zero or more project IDs for the projects to be searched for visits nearing the exception
     *                   window.
     * @return A list of all visits nearing the exception window.
     */
    @Transactional
    @Override
    public List<XnatPvisitdata> findVisitsNearException(final UserI user, final String... projectIds) {
        final Map<String, Protocol> protocols = getProtocolsForProjects(user, projectIds);
        ArrayList<XnatPvisitdata> pVisits = new ArrayList<XnatPvisitdata>();

        if (_log.isDebugEnabled()) {
            _log.debug("Found " + protocols.size() + " projects with associated protocols.");
        }

        Map<String, Protocol> projectProtocols = getProtocolsForProjects(user, projectIds);
        for (Map.Entry<String, Protocol> projectProtocolMap : projectProtocols.entrySet()){
            XnatProjectdata project = XnatProjectdata.getProjectByIDorAlias(projectProtocolMap.getKey(), (XDATUser) user, false);
            Protocol projectProtocol = projectProtocolMap.getValue();
            if(projectProtocol.getEmailNotifications().contains("overdueMissedVisit")) {
                //Only send notification emails if requested
                final Map<String, VisitReportInfo> visitReports = VisitReportResource.getVisitReport(project, projectProtocol);
                for (Map.Entry<String, VisitReportInfo> visitReport : visitReports.entrySet()) {
                    VisitReportInfo visitReportInfo = visitReport.getValue();
                    Date nextClosed = visitReportInfo.getNextClosed();
                    Date currentDate = new Date();
//                    if (currentDate.compareTo(nextClosed) > 0) {
//                        //Send an email if not yet complete. Everything returned by the query should be incomplete
//                        //Send emails to these addresses: projectProtocol.getValue().getDefaultNotificationEmails()
//                        for (String emailAddress : projectProtocol.getDefaultNotificationEmails()) {
//                            String formattedDate = new SimpleDateFormat("MMMM d, yyyy").format(visitReportInfo.getNextClosed());
//                            String body = "A visit needs to be scheduled for subject " + visitReportInfo.getSubjectId() + " as part of project " + project.getDisplayName() + ". This visit should have been scheduled for a date no later than " + formattedDate + ".";
//
//                            String subject = "Visit Overdue for " + project.getDisplayName();
//                            sendEmail(emailAddress, subject, body);
//                        }
//                    }
                    if (visitReportInfo.getStatus().equals("open")) {
                        //Send an email if period where visit should happen has started, but with no visit scheduled.
//                        //Send emails to these addresses: projectProtocol.getValue().getDefaultNotificationEmails()
                        for (String emailAddress : projectProtocol.getDefaultNotificationEmails()) {
                            String formattedDate = new SimpleDateFormat("MMMM d, yyyy").format(visitReportInfo.getNextClosed());
                            String body = "A visit needs to be scheduled very soon for subject " + visitReportInfo.getSubjectId() + " as part of project " + project.getDisplayName() + ". This visit should be scheduled for any date from now until " + formattedDate + ".";

                            String subject = "Visit Needs to be Scheduled Very Soon for " + project.getDisplayName();
                            sendEmail(emailAddress, subject, body);
                        }
                    }
                    else if (visitReportInfo.getStatus().equals("absent")) {
                        //Send an email if period where visit should happen has ended, but with no visit scheduled.
                        //Send emails to these addresses: projectProtocol.getValue().getDefaultNotificationEmails()
                        for (String emailAddress : projectProtocol.getDefaultNotificationEmails()) {
                            String formattedDate = new SimpleDateFormat("MMMM d, yyyy").format(visitReportInfo.getNextClosed());
                            String body = "A visit has not yet been scheduled for subject " + visitReportInfo.getSubjectId() + " as part of project " + project.getDisplayName() + ". This visit should have been scheduled for a date no later than " + formattedDate + ".";

                            String subject = "Visit Overdue for " + project.getDisplayName();
                            sendEmail(emailAddress, subject, body);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private Map<String, Protocol> getProtocolsForProjects(final UserI user, final String[] projectIds) {
        final Map<String, Protocol> protocols = new HashMap<>();
        if (projectIds.length == 0) {
            final List<XnatProjectdata> projects = XnatProjectdata.getAllXnatProjectdatas(user, false);
            _log.debug("Searching all system projects for configured protocols.");
            for (final XnatProjectdata project : projects) {
                final String projectId = project.getId();
                final Protocol protocol = _projectProtocolService.getProtocolForProject(projectId, user);
                if (protocol != null) {
                    if (_log.isInfoEnabled()) {
                        _log.info("Found the protocol {} configured for project {}.", protocol.getName(), projectId);
                    }
                    protocols.put(projectId, protocol);
                } else if (_log.isDebugEnabled()) {
                    _log.debug("Found no associated protocol configured for project {}.", projectId);
                }
            }
        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("Searching the specified list of projects for configured protocols: {}", Joiner.on(", ").join(projectIds));
            }
            for (final String projectId : projectIds) {
                final Protocol protocol = _projectProtocolService.getProtocolForProject(projectId, user);
                if (protocol != null) {
                    if (_log.isInfoEnabled()) {
                        _log.info("Found the protocol {} configured for project {}.", protocol.getName(), projectId);
                    }
                    protocols.put(projectId, protocol);
                } else if (_log.isWarnEnabled()) {
                    _log.warn("Found no associated protocol configured for project {} even though the project ID was passed explicitly", projectId);
                }
            }
        }
        return protocols;
    }

    private void sendEmail(String address, String subject, String body){
        try {
            _mailService.sendHtmlMessage(_preferences.getAdminEmail(), address, subject, body);
        } catch (MessagingException e) {
            _log.error("",e);
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(HibernateProtocolSchedulerService.class);

    @Inject
    private ProjectProtocolService _projectProtocolService;

    @Inject
    private MailService _mailService;

    @Autowired
    @Lazy
    private SiteConfigPreferences _preferences;
}
