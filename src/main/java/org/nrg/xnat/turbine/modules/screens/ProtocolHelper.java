/*
 * protocols: org.nrg.xnat.turbine.modules.screens.ProtocolHelper
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.screens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.display.ElementDisplay;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xnat.protocol.util.SubjectVisitInfo;

import java.util.ArrayList;
import java.util.Collections;

public class ProtocolHelper extends SecureReport {
/*
    @Inject
    private Registry _registry;

    final ModuleMetadata protocolsMetadata = _registry.getModule("protocols");
*/
    @Override
    public void finalProcessing(RunData data, Context context) {
        try {
            setupDataTypes(data, context);
            ObjectMapper mapper = new ObjectMapper();
            data.getTemplateInfo().setLayoutTemplate("/ScreenOnly.vm");
            XnatSubjectdata sub = new XnatSubjectdata(item);
            context.put("subject", sub);
            SubjectVisitInfo svi = new SubjectVisitInfo(sub, (String)context.get("project"), TurbineUtils.getUser(data));
            context.put("SubjectVisitInfo", svi);
            String visits = mapper.writeValueAsString(svi.getVisits());
            context.put("jsonVisits", visits);
            String unexpectedVisits = mapper.writeValueAsString(svi.getUnexpectedVisits());
            context.put("jsonUnexpectedVisits", unexpectedVisits);
//            String unsortedExperiments = mapper.writeValueAsString(svi.getUnsortedExperiments());
//            context.put("jsonUnsortedExperiments", unsortedExperiments);
            String jsonProtocol = mapper.writeValueAsString(svi.getProtocol());
            context.put("jsonProtocol", jsonProtocol);
        } catch (ElementNotFoundException e) {
            context.put("MissingDataType", e.ELEMENT);
        } catch (Exception e) {
            logger.error("", e);
        }
    }
/*
    public void setupModuleMetaData(RunData data, Context context) throws Exception {
        context.put("protocolsModule", protocolsMetadata);
    }
*/
    public static void setupDataTypes(RunData data, Context context) throws Exception {
        ArrayList<TypeOption> prioritizedDataTypes = new ArrayList<>();
        ArrayList<TypeOption> dataTypes = new ArrayList<>();
        ArrayList<TypeOption> assessorTypes = new ArrayList<>();
        ArrayList<ElementDisplay> elementDisplays = (ArrayList<ElementDisplay>) UserHelper.getUserHelperService(XDAT.getUserDetails()).getBrowseableCreateableElementDisplays();
        String [] imageExpPriority = {"xnat:petMrSessionData", "xnat:mrSessionData", "xnat:petSessionData"};              // prioritizes the image session sort order on the experiments table and in dropdowns
        TypeOption [] priorityDataTypes = new TypeOption [imageExpPriority.length];
        for (ElementDisplay ed : elementDisplays) {
            GenericWrapperElement gwe = ed.getSchemaElement().getGenericXFTElement();
            if (gwe.instanceOf("xnat:subjectAssessorData")) {
                int index = -1;
                for (int i=0; i<imageExpPriority.length; i++) {
                    if (imageExpPriority[i].equals(ed.getElementName())){
                        index = i;
                    }
                }
                if (index>=0){
                    priorityDataTypes[index] = new TypeOption(ed.getElementName(), ed.getSchemaElement().getSingularDescription());
                } else {
                    dataTypes.add(new TypeOption(ed.getElementName(), ed.getSchemaElement().getSingularDescription()));
                }
            }
            if (gwe.instanceOf("xnat:imageAssessorData")) {
                assessorTypes.add(new TypeOption(ed.getElementName(), ed.getSchemaElement().getSingularDescription()));
            }
        }
        Collections.sort(dataTypes);
        Collections.sort(assessorTypes);
        // cycle through priorityDataTypes and remove nulls
        for (TypeOption to : priorityDataTypes) {
            if(to != null) {
                prioritizedDataTypes.add(to);
            }
        }
        prioritizedDataTypes.addAll(dataTypes);
        context.put("dataTypeOptions", prioritizedDataTypes);
        context.put("assessorTypeOptions", assessorTypes);
        context.put("user",TurbineUtils.getUser(data));
    }

    public static class TypeOption implements Comparable<TypeOption> {
        String value, label;
        private TypeOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
        public String getValue() {
            return value;
        }
        public String getLabel() {
            return label;
        }

        @Override
        public int compareTo(TypeOption that) {
            return this.label.compareToIgnoreCase(that.label);
        }
    }
}
