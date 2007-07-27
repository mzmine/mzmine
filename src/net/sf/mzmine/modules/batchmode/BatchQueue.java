/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;

import org.dom4j.Element;

/**
 * Batch steps queue
 */
class BatchQueue extends Vector<BatchStepWrapper> implements
        StorableParameterSet {

    public static final String BATCHSTEP_ELEMENT = "batchstep";
    public static final String METHOD_ELEMENT = "method";
    public static final String PARAMETERS_ELEMENT = "parameters";

    public void exportValuesToXML(Element element) {

        for (int i = 0; i < size(); i++) {
            BatchStepWrapper step = get(i);

            Element stepElement = element.addElement(BATCHSTEP_ELEMENT);
            String methodName = step.getMethod().getClass().getName();

            Element methodElement = stepElement.addElement(METHOD_ELEMENT);
            methodElement.setText(methodName);

            Element parametersElement = stepElement.addElement(PARAMETERS_ELEMENT);
            if (step.getParameters() instanceof StorableParameterSet) {
                StorableParameterSet stepParameters = (StorableParameterSet) step.getParameters();
                stepParameters.exportValuesToXML(parametersElement);
            }

        }

    }

    public void importValuesFromXML(Element element) {

        // erase current steps
        clear();

        MZmineModule allModules[] = MZmineCore.getAllModules();

        Iterator i = element.elements(BATCHSTEP_ELEMENT).iterator();
        while (i.hasNext()) {

            Element stepElement = (Element) i.next();
            String methodName = stepElement.elementText(METHOD_ELEMENT);

            for (MZmineModule module : allModules) {
                if ((module instanceof BatchStep)
                        && (module.getClass().getName().equals(methodName))) {
                    BatchStep method = (BatchStep) module;
                    ParameterSet parameters = module.getParameterSet().clone();
                    if (parameters instanceof StorableParameterSet) {
                        Element parametersElement = stepElement.element(PARAMETERS_ELEMENT);
                        ((StorableParameterSet) parameters).importValuesFromXML(parametersElement);
                    }
                    BatchStepWrapper step = new BatchStepWrapper(method,
                            parameters);
                    add(step);
                    break;
                }
            }

        }

    }

    public BatchQueue clone() {

        BatchQueue clonedQueue = new BatchQueue();
        for (int i = 0; i < size(); i++) {
            BatchStepWrapper step = get(i);
            BatchStepWrapper clonedStep = new BatchStepWrapper(
                    step.getMethod(), step.getParameters().clone());
            clonedQueue.add(clonedStep);
        }
        return clonedQueue;
    }

}