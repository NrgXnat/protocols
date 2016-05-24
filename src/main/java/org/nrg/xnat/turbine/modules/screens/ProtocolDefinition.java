package org.nrg.xnat.turbine.modules.screens;

/*
 * org.nrg.xnat.turbine.modules.screens.ProtocolDefinition
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class ProtocolDefinition extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        if(TurbineUtils.HasPassedParameter("step",data)){
            context.put("step",TurbineUtils.GetPassedParameter("step", data));
        }
        data.getTemplateInfo().setLayoutTemplate("/ScreenOnly.vm");
    }
}
