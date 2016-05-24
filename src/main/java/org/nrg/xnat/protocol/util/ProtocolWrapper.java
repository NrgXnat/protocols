package org.nrg.xnat.protocol.util;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 2/12/14 3:20 PM
 */

import org.nrg.xnat.protocol.entities.Protocol;

import java.util.ArrayList;

public class ProtocolWrapper {
    private String protocolId;
    private ArrayList<String> userWhiteList = new ArrayList<String>();
    private Protocol protocol;
    private Integer maxVersion;

    public ProtocolWrapper(Protocol protocol) {
        this.protocol = protocol;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Integer getMaxVersion() {
        return maxVersion;
    }

    public void setMaxVersion(Integer maxVersion) {
        this.maxVersion = maxVersion;
    }
}
