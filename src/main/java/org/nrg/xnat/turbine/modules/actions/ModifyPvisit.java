package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatPvisitdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.util.ProtocolVisitSubjectHelper;
import org.nrg.xnat.utils.WorkflowUtils;

import java.util.Date;

public class ModifyPvisit extends SecureAction {
    static Logger logger = Logger.getLogger(ModifyPvisit.class);
    private ProtocolVisitSubjectHelper protocolHelper = new ProtocolVisitSubjectHelper();

    public void doPerform(RunData data, Context context) throws Exception {
        PopulateItem populater = PopulateItem.Populate(data,"xnat:pVisitData",true);
        UserI user = null; //XDAT.getUserDetails();

        Exception error=null;
        if (populater.hasError())
        {
            error = populater.getError();
        }

        XFTItem item = populater.getItem();
        XnatPvisitdata visit = new XnatPvisitdata(item);
        XnatPvisitdata existing = null;
        String projectId = (String) item.getProperty("project");
        String subjectId = (String) item.getProperty("subject_id");
        boolean terminal = "true".equals((String) item.getProperty("terminal"));
        Protocol protocol = protocolHelper.getProtocol(projectId, user);
        visit.setProtocolid(protocol.getProtocolId().toString());
        existing = XnatPvisitdata.getXnatPvisitdatasById(visit.getId(), user, false);
        // these two properties will have been double xml encoded, which causes problems with certain special characters
        visit.setVisitName(visit.getVisitName().replace("&amp;", "&"));
        if(visit.getVisitType() != null) {
            visit.setVisitType(visit.getVisitType().replace("&amp;", "&"));
        } else if(!protocol.getAllowUnexpectedAdHocVisits() && !terminal){
            error = new Exception("Error: Unexpected ad hoc visits are not allowed by the project protocol. A visit type defined in the protocol must be specified to modify or create a visit.");
        }

        // NEED TO VALIDATE THE CRAP OUT OF VISIT DATES
        if (existing != null) { // updating existing visit
            item.setProperty("ID", existing.getId());
            item.setProperty("label", existing.getLabel());
            item.setProperty("closed", existing.getClosed()); // visits cannot be opened or closed through this interface
            if(visit.getVisitType() != null) {
                VisitType visitType = protocol.getVisitType(visit.getVisitType());
                item.setProperty("terminal", visitType.getTerminal());
            }
            if(protocol.getAllowUnexpectedAdHocVisits()){
                if(terminal) {
                    item.setProperty("terminal", 1);
                } else {
                    item.setProperty("terminal", 0);
                }
            }
        }
        else if (!protocol.getAllowMultipleOpenVisits() && ProtocolVisitSubjectHelper.hasOpenVisits(projectId, subjectId, user)) {
            error = new Exception("Error: By project protocol you may not open a new visit while this subject has a previous visit still open.");
        }
        else if (ProtocolVisitSubjectHelper.hasTerminalVisit(projectId, subjectId, (Date) visit.getDate(), user)) {
            error = new Exception("Error: New visits cannot be created if they postdate a terminal visit for the subject.");
        }
        else { // creating new visit
            String newId = XnatPvisitdata.CreateNewID();
            item.setProperty("ID", newId);
            String vtype = "";
            if(visit.getVisitType() != null) {
                vtype = visit.getVisitType(); // .replaceAll("[^\\w]","");  ...don't know why this replacement was being made, but it was screwing everything up
                VisitType visitType = protocol.getVisitType(vtype);
                item.setProperty("terminal", visitType.getTerminal());
            }
            item.setProperty("label", newId + "_" + visit.getSubjectId() + "_" + visit.getVisitName().replaceAll("[^\\w]","") + "_" + vtype);;
            item.setProperty("closed", false);
            item.setProperty("visit_name", (visit.getVisitName()));
            item.setProperty("visit_type", (visit.getVisitType()));
            if(protocol.getAllowUnexpectedAdHocVisits()){
                if(terminal) {
                    item.setProperty("terminal", 1);
                } else {
                    item.setProperty("terminal", 0);
                }
            }
        }

        if(!visit.canEdit(user)){
            error = new Exception("User cannot create or modify visits for project " + projectId);
        }

        if (error!=null)
        {
            data.addMessage(error.getMessage());
            TurbineUtils.SetEditItem(item, data);
            data.setScreenTemplate("XDATScreen_edit_xnat_pVisitData.vm");
            return;
        }

        final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user,
                AutoXnatPvisitdata.SCHEMA_ELEMENT_NAME,visit.getId(),visit.getId(),
                newEventInstance(data,EventUtils.CATEGORY.PROJECT_ADMIN,EventUtils.getAddModifyAction("xnat:pVisitData", true)));
        EventMetaI c=wrk.buildEvent();

        try {
            SaveItemHelper.authorizedSave(item, user, false, true,c);

            XnatProjectdata postSave = new XnatProjectdata(item);
            postSave.getItem().setUser(user);

            postSave.initGroups();

//            user.initGroups();        // From the old XDATUser. Not sure why this was necessary.
//            user.clearLocalCache();   // From the old XDATUser. Not sure why this was necessary.

            WorkflowUtils.complete(wrk, c);
//            user.clearLocalCache();
            ElementSecurity.refresh();

            //load the subject
            XnatSubjectdata sub = new XnatSubjectdata();
            sub.setId((String) item.getProperty("subject_id"));
            XFTItem current = sub.getItem().getCurrentDBVersion();

            //prepare turbine and redirect
            data = TurbineUtils.setDataItem(data, current);
            data = TurbineUtils.SetSearchProperties(data, current);
            // fixing a problem with shared subject visit creation going back to the wrong project
            data.getParameters().setString("project", data.getParameters().getString("xnat:pvisitdata/project"));

            this.redirectToReportScreen("XDATScreen_report_xnat_subjectData.vm", current, data);
        } catch (Exception e) {
            logger.error("",e);
            WorkflowUtils.fail(wrk, c);
        }
    }

    //redirect to the subject page. Not sure if this is a good way to do that or not...
    public void postProcessing(XFTItem item, RunData data, Context context) throws Exception {

        //load the subject
        XnatSubjectdata sub = new XnatSubjectdata();
        sub.setId((String) item.getProperty("subject_id"));
        XFTItem current = sub.getItem().getCurrentDBVersion();

        //prepare turbine and redirect
        data = TurbineUtils.setDataItem(data, current);
        data = TurbineUtils.SetSearchProperties(data, current);

        this.redirectToReportScreen("XDATScreen_report_xnat_subjectData.vm", current, data);

    }

}
