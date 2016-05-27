package org.nrg.xnat.turbine.modules.screens;

/*
 * org.nrg.xnat.turbine.modules.screens.ManageProtocol
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.display.ElementDisplay;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;

import java.util.ArrayList;
import java.util.Collections;

public class ManageProtocol extends SecureScreen {
    public final static Logger logger = Logger.getLogger(ManageProtocol.class);
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        context.put("user",TurbineUtils.getUser(data));
        // For 1.7 and above the module registry feature should exist and we shouldn't need to do this for one specific page like below
//        new ProtocolHelper().setupModuleMetaData(data, context);


        /*

        $data.getSession().getAttribute("user").isSiteAdmin())
        #if (!$canManageProtocols)
            #foreach($authority in $data.getSession().getAttribute("user").getAuthorities())

                authority: $authority

                #if ($authority.getAuthority().endsWith("_owner"))

         */
    }
}
