package org.nrg.xnat.protocol.services;

import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.ProjectProtocol;

import java.util.List;

/**
 * The protocol schedule service searches for visits that may require intervention for scheduling purposes.
 * Specifically, it can look for visits that are near scheduling, i.e. fall within a window where the visit should be
 * scheduled with the subject. It can also look for visits that are near exception, i.e. are drawing close to the time
 * when the visit will be out of protocol and generate a protocol exception.
 */
public interface ProtocolSchedulerService {
    /**
     * This method finds any visits for the specified projects that are within the window specified by the {@link
     * ProjectProtocol project protocol} and have not yet been scheduled. If no project IDs are specified, then all
     * projects with associated protocols are searched.
     * @param projectIds    Zero or more project IDs for the projects to be searched for visits nearing the scheduling
     *                      window.
     * @return A list of all visits nearing the scheduling window.
     */
    List<XnatPvisitdata> findVisitsNearScheduling(final UserI user, final String... projectIds);
    /**
     * This method finds any visits for the specified projects that are nearing the allowable delta drift specified by
     * the {@link ProjectProtocol project protocol} and have not yet been complete. If no project IDs are specified,
     * then all projects with associated protocols are searched.
     * @param projectIds    Zero or more project IDs for the projects to be searched for visits nearing the exception
     *                      window.
     * @return A list of all visits nearing the exception window.
     */
    List<XnatPvisitdata> findVisitsNearException(final UserI user, final String... projectIds);
}
