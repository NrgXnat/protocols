package org.nrg.xnat.protocol.util;/*
 * org.nrg.xnat.protocol.util.SubjectVisitInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 9/12/13 1:27 PM
 */

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
