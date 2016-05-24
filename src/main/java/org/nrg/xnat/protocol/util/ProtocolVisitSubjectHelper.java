package org.nrg.xnat.protocol.util;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xft.XFTTable;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.search.TableSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.subentities.ExpectedExperiment;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.services.ProjectProtocolService;
import org.nrg.xnat.protocol.services.ProtocolExceptionService;
import org.nrg.xnat.protocol.services.ProtocolService;
import org.nrg.xnat.utils.XnatUserProvider;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProtocolVisitSubjectHelper {
    //protected Map<XnatPvisitdata, List<ExperimentAssessorContainer>> visits = null;

    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ProtocolVisitSubjectHelper.class);

    public ProtocolVisitSubjectHelper() {

    }

    public Protocol getProtocol(String projectID, UserI user) {
        if (projectID == null) {
            return null;
        }

        final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectID, user, false);
        if (p == null) return null;

        Protocol protocol = _projectProtocolService.getProtocolForProject(projectID, user);
        if (protocol == null) {
            // probably just doesn't have a protocol
            logger.debug("no protocol found for this project: " + projectID);
            return null;
        }
        String validateString = protocol.validate();
        if (validateString != null) {
            logger.error("the protocol is invalid: " + validateString);
            return null;
        }
        return protocol;
    }

    public boolean visitHasExperimentOrException(String visitId, String xsiType, String subtype, UserI user, boolean exceptionsAllowed) {
        boolean hasExperiment = visitHasExperiment(visitId, xsiType, subtype, user);
        boolean hasException = _protocolExceptionService.findExceptionForVisitAndType(visitId, xsiType, subtype) != null;
        return hasExperiment || (exceptionsAllowed && hasException);
    }

    public static boolean visitHasExperiment(String visitId, String xsiType, String subtype, UserI user) {
        try {
            XFTTable table = TableSearch.Execute("SELECT * " +
                    "FROM xnat_experimentData ex " +
                    "INNER JOIN xdat_meta_element me ON ex.extension=me.xdat_meta_element_id " +
                    "WHERE ex.visit = '" + visitId + "' " +
                    "AND me.element_name = '" + xsiType +
                    (subtype != null ? "' AND ex.protocol = '" + subtype.replace("\\", "\\\\") + "';" : "' AND ex.protocol IS NULL;"),
                    null, null);
            table.resetRowCursor();


            return table.hasMoreRows();
        }
        catch (java.lang.Exception e) {
            logger.error("", e);
        }

        return false;
    }

    public static boolean hasOpenVisits(String projectId, String subjectId, UserI user) {
        try {
            XFTTable table = TableSearch.Execute("SELECT * " +
                    "FROM xnat_experimentData ex " +
                    "INNER JOIN xnat_pvisitdata vd ON ex.id=vd.id " +
                    "WHERE ex.project = '" + projectId + "' " +
                    "AND vd.subject_id = '" + subjectId + "' " +
                    "AND (vd.closed = 0 OR vd.closed IS NULL);",
                    null, null);
            table.resetRowCursor();
            return table.hasMoreRows();
        }
        catch (java.lang.Exception e) {
            logger.error("", e);
        }

        return false;
    }

    public static boolean hasTerminalVisit(String projectId, String subjectId, Date date, UserI user) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");

            XFTTable table = TableSearch.Execute("SELECT * " +
                    "FROM xnat_experimentData ex " +
                    "INNER JOIN xnat_pvisitdata vd ON ex.id=vd.id " +
                    "WHERE ex.project = '" + projectId + "' " +
                    (date != null ? ("AND ex.date < '" + sdf.format(date) + "' ") : "") +
                    "AND vd.subject_id = '" + subjectId + "' " +
                    "AND vd.terminal = 1;",
                    null, null);
            table.resetRowCursor();
            return table.hasMoreRows();
        }
        catch (java.lang.Exception e) {
            logger.error("", e);
        }

        return false;
    }

    public static XnatPvisitdata getVisitByVisitName(String projectId, String subjectId, String visitName, UserI user) {
        try {
            XFTTable table = TableSearch.Execute("SELECT vd.id AS visit_id " +
                    "FROM xnat_experimentData ex " +
                    "INNER JOIN xnat_pvisitdata vd ON ex.id=vd.id " +
                    "WHERE ex.project = '" + projectId + "' " +
                    "AND vd.subject_id = '" + subjectId + "' " +
                    "AND vd.visit_name = '" + visitName.replace("\\", "\\\\") + "';",
                    null, null);
            table.resetRowCursor();
            if (table.hasMoreRows()) {
                Hashtable row = table.nextRowHash();
                final Object visit_id = row.get("visit_id");
                XnatPvisitdata visit = new XnatPvisitdata(ItemSearch.GetItem("xnat:pVisitData/id", visit_id, null, true));
                return visit;
            }
        }
        catch (java.lang.Exception e) {
            logger.error("", e);
        }
        return null;
    }

    public Collection<String> getSubtypesForVisit(String visitId, UserI user) {
        Set<String> subtypes = new HashSet<String>();
        try {
            XnatPvisitdata visit = new XnatPvisitdata(ItemSearch.GetItem("xnat:pVisitData/id", visitId, null, true));
            Protocol protocol = getProtocol(visit.getProject(), user);
            if (protocol != null && visit != null) {
                String visitTypeName = visit.getVisitType();
                for (VisitType visitType : protocol.getVisitTypes()) {
                    if (visitType.getName().equals(visitTypeName)) {
                        for (ExpectedExperiment expectedExperiment : visitType.getExpectedExperiments()) {
                            if (!StringUtils.isEmpty(expectedExperiment.getSubtype())) {
                                subtypes.add(expectedExperiment.getSubtype());
                            }
                        }
                    }
                }
            }
        } catch (java.lang.Exception e) {
            logger.error("", e);
        }
        return subtypes;
    }

    @Inject
    private ProtocolExceptionService _protocolExceptionService;

    @Inject
    private ProjectProtocolService _projectProtocolService;

}
