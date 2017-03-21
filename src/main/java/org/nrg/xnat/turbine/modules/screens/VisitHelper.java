/*
 * protocols: org.nrg.xnat.turbine.modules.screens.VisitHelper
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;

public class VisitHelper extends SecureReport {

    @Override
    public void finalProcessing(RunData data, Context context) {
        try {
            data.getTemplateInfo().setLayoutTemplate("/ScreenOnly.vm");

            XnatPvisitdata visit = new XnatPvisitdata(item);
            SubjectVisitInfo subjectVisitInfo = new SubjectVisitInfo(visit.getSubjectData(), (String)context.get("project"), TurbineUtils.getUser(data));
            SubjectVisitInfo.VisitInfo visitInfo = null;
            String visitId = visit.getId();
            for (SubjectVisitInfo.VisitInfo vi : subjectVisitInfo.getVisits()) {
                if (visitId.equals(vi.getId())) {
                    visitInfo = vi;
                }
            }
            if (visitInfo == null) {
                for (SubjectVisitInfo.VisitInfo vi : subjectVisitInfo.getUnexpectedVisits()) {
                    if (visitId.equals(vi.getId())) {
                        visitInfo = vi;
                    }
                }
            }

            context.put("subject", visit.getSubjectData());
            context.put("visit", visitInfo);
            context.put("protocol", subjectVisitInfo.getProtocol());
        } catch (Exception e) {
            logger.error("", e);
        }
    }
}
