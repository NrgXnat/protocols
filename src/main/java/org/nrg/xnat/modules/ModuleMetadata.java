package org.nrg.xnat.modules;

import java.text.DateFormat;
import java.util.Date;

public class ModuleMetadata {
    private String _moduleId;
    private String _changeset;
    private String _timestamp;
    private Date timestamp;

    public String getModuleId() {
        return _moduleId;
    }

    public void setModuleId(String moduleId) {
        _moduleId = moduleId;
    }

    public String getChangeset() {
        return _changeset;
    }

    public void setChangeset(String changeset) {
        _changeset = changeset;
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public void setTimestamp(String timestamp) {
        this._timestamp = timestamp;
        long ts = Long.parseLong(timestamp);
        this.timestamp = new Date(ts);
    }

    public String getBuildDate() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        return df.format(this.timestamp);
    }
}
