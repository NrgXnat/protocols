/*
 * protocols: org.nrg.xnat.protocol.util.FindVisitsNearSchedulingOrExceptionJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.util;

import org.nrg.schedule.JobInterface;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.services.ProtocolSchedulerService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class FindVisitsNearSchedulingOrExceptionJob implements JobInterface {
    @Override
    public void init(final JobExecutionContext context) throws JobExecutionException {
        _map = context.getMergedJobDataMap();
    }

    @Override
    public void execute() throws JobExecutionException {
        final List<XnatPvisitdata> impendings = _service.findVisitsNearScheduling(getUser());
        if (_log.isDebugEnabled()) {
            _log.debug("Found {} visits with impending scheduling windows.", impendings.size());
        }
        final List<XnatPvisitdata> exceptions = _service.findVisitsNearException(getUser());
        if (_log.isDebugEnabled()) {
            _log.debug("Found {} visits with impending exception deadlines.", exceptions.size());
        }
    }

    @Override
    public void destroy() {

    }

    @SuppressWarnings("unchecked")
    private UserI getUser() {
        final XDATUser user = ((Provider<XDATUser>) _map.get("user")).get();
        if (_log.isDebugEnabled()) {
            _log.debug("Retrieved user {} based on mapped job data settings.", user.getLogin());
        }
        return user;
    }

    private static final Logger _log = LoggerFactory.getLogger(FindVisitsNearSchedulingOrExceptionJob.class);

    @Inject
    private ProtocolSchedulerService _service;

    private JobDataMap _map;
}
