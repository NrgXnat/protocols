/*
 * protocols: org.nrg.xnat.screens.uploadApplet.context.VisitContextAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.screens.uploadApplet.context;

import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class VisitContextAction {
    public void execute(RunData data, Context context) {
        if (!StringUtils.isEmpty((String) TurbineUtils.GetPassedParameter("visit", data))) {
            context.put("visit", StringUtils.trimToEmpty((String)TurbineUtils.GetPassedParameter("visit",data)));
        }

        if (!StringUtils.isEmpty((String)TurbineUtils.GetPassedParameter("subtype",data))) {
            context.put("subtype", StringUtils.trimToEmpty((String)TurbineUtils.GetPassedParameter("subtype",data)).replace("&amp;", "&"));
        }
    }
}
