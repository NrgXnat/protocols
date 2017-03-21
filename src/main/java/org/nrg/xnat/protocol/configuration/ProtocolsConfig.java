/*
 * protocols: org.nrg.xnat.protocol.configuration.ProtocolsConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.configuration;

import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.context.annotation.ComponentScan;

@XnatPlugin(value = "protocols", name = "XNAT Protocols Plugin", entityPackages = {"org.nrg.xnat.protocol.entities", "org.nrg.xnat.protocol.entities.subentities"})
@ComponentScan({"org.nrg.xnat.protocol.services.impl", "org.nrg.xnat.protocol.daos"})
public class ProtocolsConfig {

}
