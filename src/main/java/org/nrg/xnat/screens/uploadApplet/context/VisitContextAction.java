package org.nrg.xnat.screens.uploadApplet.context;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 4/8/14 2:38 PM
 */

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
