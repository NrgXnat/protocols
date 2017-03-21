/*
 * protocols: org.nrg.xnat.actions.sessionEdit.compare.VisitClosedCompareAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.actions.sessionEdit.compare;

import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.modules.actions.ModifySubjectAssessorData;

public class VisitClosedCompareAction implements ModifySubjectAssessorData.CompareAction {

    public void execute(UserI user, XFTItem from, XFTItem to) throws Exception {
        XnatPvisitdata fromVisit = XnatPvisitdata.getXnatPvisitdatasById(from.getProperty("visit"), user, false);
        XnatPvisitdata toVisit = XnatPvisitdata.getXnatPvisitdatasById(to.getProperty("visit"), user, false);
        if ((fromVisit == null && toVisit != null) || (fromVisit != null && toVisit == null) || (fromVisit != null && toVisit != null && !fromVisit.getId().equals(toVisit.getId()))) {
            if (fromVisit != null && fromVisit.getClosed()) {
                throw new ModifySubjectAssessorData.CompareException("Experiments may not be removed from a visit that has been closed.");
            }
            if (toVisit != null && toVisit.getClosed()) {
                throw new ModifySubjectAssessorData.CompareException("Experiments may not be added to a visit that has been closed.");
            }
        }
    }
}
