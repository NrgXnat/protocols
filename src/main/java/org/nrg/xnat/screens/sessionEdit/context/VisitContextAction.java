package org.nrg.xnat.screens.sessionEdit.context;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 4/8/14 2:50 PM
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.modules.screens.EditSubjectAssessorScreen;

public class VisitContextAction implements EditSubjectAssessorScreen.ContextAction {
    public void execute(RunData data, Context context, ItemI item) {
        XnatSubjectassessordata assessor= new XnatSubjectassessordata(item);
        if(assessor != null && assessor.getVisit() == null && TurbineUtils.HasPassedParameter("visit", data)) {
            XnatPvisitdata xnatPvisitdata = XnatPvisitdata.getXnatPvisitdatasById(TurbineUtils.GetPassedParameter("visit", data), null, false);
            context.put("visit_name", xnatPvisitdata.getVisitName());
            assessor.setVisit((String) TurbineUtils.GetPassedParameter("visit", data));
            if(assessor.getSubjectId() == null){
                assessor.setSubjectId(xnatPvisitdata.getSubjectId());
            }
        }
        else if (assessor != null && assessor.getVisit() != null) {
            XnatPvisitdata xnatPvisitdata = XnatPvisitdata.getXnatPvisitdatasById(assessor.getVisit(), null, false);
            context.put("visit_name", xnatPvisitdata.getVisitName());
        }
        else if (!StringUtils.isEmpty((String)context.get("visit_name"))) {
            // visit_name will have been HTML encoded by the link generator, which is redundant since it's already XML encoded
            context.put("visit_name", ((String) context.get("visit_name")).replace("&amp;", "&"));
        }
        context.put("user", TurbineUtils.getUser(data));
    }
}
