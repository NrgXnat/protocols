/*
 * protocols: org.nrg.xnat.project.getBundles.extensions.ProtocolBundle
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.project.getBundles.extensions;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.collections.DisplayFieldCollection;
import org.nrg.xdat.display.ElementDisplay;
import org.nrg.xdat.display.SQLQueryField;
import org.nrg.xdat.om.XdatSearchField;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.search.CriteriaCollection;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.nrg.xnat.protocol.entities.Protocol;
import org.nrg.xnat.protocol.entities.subentities.ExpectedExperiment;
import org.nrg.xnat.protocol.entities.subentities.VisitType;
import org.nrg.xnat.protocol.services.ProjectProtocolService;

import javax.inject.Inject;

/**
 * Created by jcleve01 on 2/25/15.
 */
public class ProtocolBundle implements BaseXnatProjectdata.AddBundlesI {
    static Logger logger = Logger.getLogger(ProtocolBundle.class);

    UserI user;
    List<XdatStoredSearch> storedSearches;
    BaseXnatProjectdata proj;
    Protocol protocol;

    @Override
    public void execute(UserI user, List<XdatStoredSearch> storedSearches, BaseXnatProjectdata proj) throws Exception {
        this.user = user;
        this.storedSearches = storedSearches;
        this.proj = proj;

        // Determine whether or not this project even has a protocol
        if(_projectProtocolService == null) {
           return;
        }
        this.protocol = _projectProtocolService.getProtocolForProject(proj.getId(), user);
        if(protocol == null){
            return;
        }

        // Runs once!
        confirmDataTypes();

        XFTTable visitTypes = XFTTable.Execute(
            "SELECT visit_type, visit_name, MAX(insert_date) AS insert_date from xnat_pvisitdata LEFT JOIN xnat_experimentData expt ON xnat_pvisitdata.id=expt.id LEFT JOIN xnat_pvisitdata_meta_data meta ON xnat_pvisitdata.pvisitData_info=meta.meta_data_id WHERE expt.project='"+proj.getId()+"' GROUP BY visit_type, visit_name ORDER BY MAX(insert_date) ASC;"
            , null, null);
        List<Hashtable> visits = visitTypes.toArrayListOfHashtables();


        // Getting rid of the default generated pvisit stored search...
        final List<XdatStoredSearch> visitSearches = new ArrayList<>();
        for(final XdatStoredSearch ss: storedSearches){
            if(ss.getId().equals("@"+XnatPvisitdata.SCHEMA_ELEMENT_NAME)){
                visitSearches.add(ss);
            }
        }
        storedSearches.removeAll(visitSearches);
        
        List<Hashtable> newTypes = Lists.newArrayList();
        for(Hashtable visit: visits) {
            boolean matched = false;
            for(XdatStoredSearch xss: storedSearches){
                if(StringUtils.equals(xss.getTag(), proj.getId()) && StringUtils.equals(xss.getBriefDescription(), "Visit: "+visit.get("visit_name"))){
                    matched = true;
                }
            }

            if(matched){
                continue;
            }

            XdatStoredSearch xss = buildSearch(visit);
            if(xss == null){
                continue;
            }
            xss.setId("vp_"+Calendar.getInstance().getTimeInMillis());

            xss.setTag(proj.getId());
            SaveItemHelper.authorizedSave(xss, user, true, true,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, EventUtils.TYPE.WEB_SERVICE, "Registered Protocol View")
            );
            newTypes.add(visit);
            this.storedSearches.add(xss);
        }

        Collections.sort(this.storedSearches, new Comparator<XdatStoredSearch>() {
            @Override
            public int compare(XdatStoredSearch o1, XdatStoredSearch o2) {
                if(o1 != null && o1.getBriefDescription() != null){
                    if(o1.getBriefDescription().equals("Visit: All")){
                        return -1;
                    }
                    if(o2.getBriefDescription() != null && o2.getBriefDescription().equals("Visit: All")){
                        return 1;
                    }
                    return o1.getBriefDescription().compareTo(o2.getBriefDescription());
                } else {
                    return -1;
                }
            }
        });

        // build an "all visits" tab
        XdatStoredSearch matched = null;
        for(XdatStoredSearch xss: storedSearches){
            if(StringUtils.equals(xss.getTag(), proj.getId()) && StringUtils.equals(xss.getBriefDescription(), "Visit: All")){
                matched = xss;
            }
        }

        if (matched != null && newTypes.size() == 0) {
            return;
        } else if(matched != null) {
            visits = newTypes;
        }

        XdatStoredSearch xss = buildSearch(visits, matched);
        if(xss == null){
            return;
        }
        xss.setId("vp_"+Calendar.getInstance().getTimeInMillis());

        xss.setTag(proj.getId());
        SaveItemHelper.authorizedSave(xss, user, true, true,
                EventUtils.newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, EventUtils.TYPE.WEB_SERVICE, "Registered Protocol View")
        );
        this.storedSearches.add(0,xss);
    }

    public XdatStoredSearch buildSearch(Hashtable visitInfo) throws Exception {
        DisplaySearch ds = new DisplaySearch();
        ds.setRootElement(XnatPvisitdata.SCHEMA_ELEMENT_NAME);
        ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, "SUB_PROJECT_IDENTIFIER", "Subject", proj.getId());
        ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, "DATE");

        VisitType vt = protocol.getVisitType((String) visitInfo.get("visit_type"));
        if(vt == null) {
            return null;
        }
        for(ExpectedExperiment exp: vt.getExpectedExperiments()) {
            String type = exp.getType();
            String subtype = exp.getSubtype();
            SchemaElement se = SchemaElement.GetElement(type);

            String header = se.getPluralDescription();
            if(StringUtils.isBlank(subtype)){
                subtype = "x";
            } else {
                header = subtype;
            }
            ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, (subtype.equals("x")?"VP2_":"VP_")+se.getSQLName(), header, subtype);
        }

        CriteriaCollection cc = new CriteriaCollection("OR");
        cc.addClause(XnatPvisitdata.SCHEMA_ELEMENT_NAME+"/sharing/share/project", "=", proj.getId());
        cc.addClause(XnatPvisitdata.SCHEMA_ELEMENT_NAME+".PROJECT", "=", proj.getId());
        ds.addCriteria(cc);
        ds.addCriteria(XnatPvisitdata.SCHEMA_ELEMENT_NAME+"/visit_name", "=", visitInfo.get("visit_name"));

        XdatStoredSearch xss = ds.convertToStoredSearch("");
        xss.setBriefDescription("Visit: "+visitInfo.get("visit_name"));
        return xss;
    }

    public XdatStoredSearch buildSearch(List<Hashtable> visits, XdatStoredSearch matched) throws Exception {
        if(matched == null){
            DisplaySearch ds = new DisplaySearch();
            ds.setRootElement(XnatPvisitdata.SCHEMA_ELEMENT_NAME);
            ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, "SUB_PROJECT_IDENTIFIER", "Subject", proj.getId());
            ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, "DATE");
            for(Hashtable visitInfo: visits){
                VisitType vt = protocol.getVisitType((String) visitInfo.get("visit_type"));
                if(vt == null) {
                    continue;
                }
                for(ExpectedExperiment exp: vt.getExpectedExperiments()) {
                    String type = exp.getType();
                    String subtype = exp.getSubtype();
                    SchemaElement se = SchemaElement.GetElement(type);

                    String header = se.getPluralDescription();
                    if(StringUtils.isBlank(subtype)){
                        subtype = "x";
                    } else {
                        header = subtype;
                    }
                    ds.addDisplayField(XnatPvisitdata.SCHEMA_ELEMENT_NAME, (subtype.equals("x")?"VP2_":"VP_")+se.getSQLName(), vt.getName()+" "+header, subtype);
                }
            }

            CriteriaCollection cc = new CriteriaCollection("OR");
            cc.addClause(XnatPvisitdata.SCHEMA_ELEMENT_NAME+"/sharing/share/project", "=", proj.getId());
            cc.addClause(XnatPvisitdata.SCHEMA_ELEMENT_NAME + ".PROJECT", "=", proj.getId());
            ds.addCriteria(cc);

            XdatStoredSearch xss = ds.convertToStoredSearch("");
            xss.setBriefDescription("Visit: All");
            return xss;
        } else {
            for(Hashtable visitInfo: visits){
                VisitType vt = protocol.getVisitType((String) visitInfo.get("visit_type"));
                if(vt == null) {
                    continue;
                }
                for(ExpectedExperiment exp: vt.getExpectedExperiments()) {
                    String type = exp.getType();
                    String subtype = exp.getSubtype();
                    SchemaElement se = SchemaElement.GetElement(type);

                    String header = se.getPluralDescription();
                    if(StringUtils.isBlank(subtype)){
                        subtype = "x";
                    } else {
                        header = subtype;
                    }
                    XdatSearchField sf = new XdatSearchField(user);
                    sf.setElementName(XnatPvisitdata.SCHEMA_ELEMENT_NAME);
                    sf.setFieldId((subtype.equals("x") ? "VP2_" : "VP_") + se.getSQLName());
                    sf.setHeader(vt.getName() + " " + header);
                    sf.setValue(subtype);
                    sf.setType("string");
                    sf.setSequence(new Integer(matched.getSearchFields().size()));
                    matched.setSearchField(sf);
                }
            }
            return matched;
        }
    }

    private static boolean confirmed = false;
    private static void confirmDataTypes() {
        if(!confirmed){
            try {
/*
                XFTTable views = XFTTable.Execute(
                    "SELECT lower(c.relname) AS relname FROM pg_catalog.pg_class AS c LEFT JOIN pg_catalog.pg_namespace AS n ON n.oid = c.relnamespace WHERE c.relkind IN ('v') AND n.nspname NOT IN ('pg_catalog', 'pg_toast') AND pg_catalog.pg_table_is_visible(c.oid) AND relname LIKE 'vp_%'",
                    null, null);
                List<String> viewsList = views.convertColumnToArrayList("relname");
*/
                SchemaElement pvisit = SchemaElement.GetElement(XnatPvisitdata.SCHEMA_ELEMENT_NAME);
                ElementDisplay ed = pvisit.getDisplay();
                for(ElementSecurity es: ElementSecurity.GetSecureElements()){
                    SchemaElement se = es.getSchemaElement();
/*
                    if(!viewsList.contains("vp_"+se.getSQLName().toLowerCase()) && se.instanceOf("xnat:experimentData")){
                        PoolDBUtils.ExecuteNonSelectQuery(
                            "CREATE VIEW VP_"+se.getSQLName()+" AS SELECT ID, visit, protocol FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id WHERE element_name='"+es.getElementName()+"' AND visit is not null"
                            , null, null);
                    }
*/
                    String absent = "<span class=\"icon icon-sm icon-status icon-add\" onclick=\"addExp(''' || visit.id || ''',''"+se.getFullXMLName()+"'',''@WHERE'');\"></span>";
                    if(se.instanceOf("xnat:imageSessionData")){
                        absent = "<span class=\"icon icon-sm icon-status icon-add\" onclick=\"addImages(''' || visit.id || ''',''"+se.getFullXMLName()+"'',''@WHERE'');\"></span>";
                    }
                    String found = "<span class=\"icon icon-sm icon-status icon-status-complete\" onclick=\"return rpt(''' || expt.id || ''',''"+se.getFullXMLName()+"'',''"+se.getFullXMLName()+".ID'');\"></span>";
                    String exception = "<span class=\"icon icon-sm icon-status icon-status-exception tip_icon\"><span class=\"tip shadowed\">' || exceptions._explanation || '</span></span>";
                    if (se.instanceOf("xnat:experimentData")) {
                        SQLQueryField sqf = new SQLQueryField(ed);
                        sqf.setId("VP_"+se.getSQLName());
                        sqf.setHeader("Experiments");
                        sqf.setVisible(true);
                        sqf.setSearchable(true);
                        sqf.setDataType("string");
                        sqf.setSubQuery("SELECT visit.id as visit, coalesce(experiments.id, exceptions.status, '"+absent+"') as id FROM xnat_pVisitData visit LEFT JOIN (SELECT '"+found+"' as id, visit FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id WHERE element_name='" + es.getElementName() + "' AND visit IS NOT NULL AND protocol = '@WHERE') experiments on visit.id=experiments.visit LEFT JOIN (SELECT _visit_id, '"+exception+"'::text as status FROM xhbm_protocol_exception exceptions WHERE exceptions._xsi_type='" + es.getElementName() + "' AND exceptions._subtype='@WHERE') exceptions on visit.id=exceptions._visit_id");
                        sqf.addMappingColumn(XnatPvisitdata.SCHEMA_ELEMENT_NAME+"/ID", "visit");
                        Hashtable content = new Hashtable();
                        content.put("sql", "id");
                        sqf.setContent(content);
                        try {
                            ed.addDisplayFieldWException(sqf);
                        } catch (DisplayFieldCollection.DuplicateDisplayFieldException e) {
                            logger.error(sqf.getParentDisplay().getElementName() + "." + sqf.getId());
                            logger.error("",e);
                        }

                        sqf = new SQLQueryField(ed);
                        sqf.setId("VP2_"+se.getSQLName());
                        sqf.setHeader("Experiments");
                        sqf.setVisible(true);
                        sqf.setSearchable(true);
                        sqf.setDataType("string");
                        sqf.setSubQuery("SELECT visit.id as visit, coalesce(experiments.id, exceptions.status, '"+absent.replace("@WHERE","")+"') as id FROM xnat_pVisitData visit LEFT JOIN (SELECT '"+found+"' as id, visit FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id WHERE element_name='" + es.getElementName() + "' AND visit IS NOT NULL AND protocol is null) experiments on visit.id=experiments.visit LEFT JOIN (SELECT _visit_id, '"+exception+"'::text as status FROM xhbm_protocol_exception exceptions WHERE exceptions._xsi_type='" + es.getElementName() + "' AND exceptions._subtype is null) exceptions on visit.id=exceptions._visit_id");
                        sqf.addMappingColumn(XnatPvisitdata.SCHEMA_ELEMENT_NAME + "/ID", "visit");
                        content = new Hashtable();
                        content.put("sql", "id");
                        sqf.setContent(content);
                        try {
                            ed.addDisplayFieldWException(sqf);
                        } catch (DisplayFieldCollection.DuplicateDisplayFieldException e) {
                            logger.error(sqf.getParentDisplay().getElementName() + "." + sqf.getId());
                            logger.error("",e);
                        }
                    }
                }
                confirmed = true;
            } catch (DBPoolException e) {
                logger.error("", e);
            } catch (SQLException e) {
                logger.error("", e);
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Inject
    private ProjectProtocolService _projectProtocolService;
}
