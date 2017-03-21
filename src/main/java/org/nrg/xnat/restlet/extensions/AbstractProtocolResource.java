/*
 * protocols: org.nrg.xnat.restlet.extensions.AbstractProtocolResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.extensions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.protocol.services.ProjectProtocolService;
import org.nrg.xnat.protocol.services.ProtocolExceptionService;
import org.nrg.xnat.protocol.services.ProtocolLineageService;
import org.nrg.xnat.protocol.services.ProtocolService;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public abstract class AbstractProtocolResource extends SecureResource {

    protected ProtocolService protocolService = null;
    protected ProjectProtocolService projectProtocolService = null;
    protected ProtocolExceptionService protocolExceptionService = null;
    protected ProtocolLineageService protocolLineageService = null;

    protected final ObjectMapper mapper = new ObjectMapper();

    public AbstractProtocolResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    protected ProtocolService getProtocolService() {
        if (protocolService == null) {
            protocolService = XDAT.getContextService().getBean(ProtocolService.class);
        }
        return protocolService;
    }

    protected ProjectProtocolService getProjectProtocolService() {
        if (projectProtocolService == null) {
            projectProtocolService = XDAT.getContextService().getBean(ProjectProtocolService.class);
        }
        return projectProtocolService;
    }

    protected ProtocolExceptionService getProtocolExceptionService() {
        if (protocolExceptionService == null) {
            protocolExceptionService = XDAT.getContextService().getBean(ProtocolExceptionService.class);
        }
        return protocolExceptionService;
    }

    protected ProtocolLineageService getProtocolLineageService() {
        if (protocolLineageService == null) {
            protocolLineageService = XDAT.getContextService().getBean(ProtocolLineageService.class);
        }
        return protocolLineageService;
    }
}
