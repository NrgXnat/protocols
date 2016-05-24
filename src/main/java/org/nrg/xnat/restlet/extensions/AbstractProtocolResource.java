package org.nrg.xnat.restlet.extensions;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 2/12/14 4:10 PM
 */


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

import javax.inject.Inject;

public abstract class AbstractProtocolResource extends SecureResource {
    protected final ObjectMapper mapper = new ObjectMapper();

    public AbstractProtocolResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Inject
    protected ProtocolService _protocolService;

    @Inject
    protected ProjectProtocolService _projectProtocolService;

    @Inject
    protected ProtocolExceptionService _protocolExceptionService;

    @Inject
    protected ProtocolLineageService _protocolLineageService;
}
