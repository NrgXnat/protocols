/*
 * protocols: org.nrg.xnat.protocol.util.VisitReportInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.protocol.util;

import java.sql.Date;

public class VisitReportInfo {

    String subjectId;
    String status;
    java.sql.Date nextOpen;
    java.sql.Date nextClosed;

    public String getSubjectId() {
        return subjectId;
    }

    public String getStatus() {
        return status;
    }

    public java.sql.Date getNextOpen() {
        return nextOpen;
    }

    public java.sql.Date getNextClosed() {
        return nextClosed;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNextOpen(Date nextOpen) {
        this.nextOpen = nextOpen;
    }

    public void setNextClosed(Date nextClosed) {
        this.nextClosed = nextClosed;
    }

}
