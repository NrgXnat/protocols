/*
 * protocols: org.nrg.xnat.protocol.tasks.ProtocolScheduledTask
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.tasks;

/**
 * Created by jcleve01 on 5/20/2016.
 */
public class ProtocolScheduledTask implements Runnable {
    @Override
    public void run() {
System.out.println("DO STUFF HERE!");
    }
}
