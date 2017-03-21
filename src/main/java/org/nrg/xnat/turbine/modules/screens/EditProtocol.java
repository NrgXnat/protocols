/*
 * protocols: org.nrg.xnat.turbine.modules.screens.EditProtocol
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class EditProtocol extends SecureScreen {
    public final static Logger logger = Logger.getLogger(EditProtocol.class);
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ProtocolHelper.setupDataTypes(data, context);
    }
}
