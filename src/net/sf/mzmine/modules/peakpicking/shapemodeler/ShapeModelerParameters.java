/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.shapemodeler;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class ShapeModelerParameters implements StorableParameterSet {

    private static final String PARAMETER_NAME_ATTRIBUTE = "name";

    // Peak recognition
    public static final String shapeModelerNames[] = { "Triangle",
            "Gaussian", "EMG"};

    public static final String shapeModelerClasses[] = {
            "net.sf.mzmine.modules.peakpicking.shapemodeler.peakmodels.TrianglePeakModel",
            "net.sf.mzmine.modules.peakpicking.shapemodeler.peakmodels.GaussianPeakModel",
            "net.sf.mzmine.modules.peakpicking.shapemodeler.peakmodels.EMGPeakModel" };

    public static final String shapeModelerHelpFiles[] = {
        "net/sf/mzmine/modules/peakpicking/shapemodeler/peakmodels/TrianglePeakModel.html",
        "net/sf/mzmine/modules/peakpicking/shapemodeler/peakmodels/GaussianPeakModel.html",
        "net/sf/mzmine/modules/peakpicking/shapemodeler/peakmodels/EMGPeakModel.html" };

    // Three step parameters
    private SimpleParameterSet shapeModelerParameters[], myParameters;

    private static final Parameter shapeModelerTypeNumber = new SimpleParameter(
            ParameterType.INTEGER,
            "Shape modeler type",
            "This value defines the type of shape modeler to use",
            0);

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Suffix",
            "This string is added to filename as suffix", "resolved");

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only resolved version remains",
            new Boolean(false));

    public ShapeModelerParameters() {

        shapeModelerParameters = new SimpleParameterSet[shapeModelerClasses.length];

        myParameters = new SimpleParameterSet(new Parameter[] {
                shapeModelerTypeNumber, suffix, autoRemove });

        for (int i = 0; i < shapeModelerClasses.length; i++) {
            String className = shapeModelerClasses[i] + "Parameters";
            Class paramClass;
            try {
                paramClass = Class.forName(className);
                shapeModelerParameters[i] = (SimpleParameterSet) paramClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @param int index
     * @return SimpleParameterSet
     */
    public SimpleParameterSet getPeakModelerParameters(int ind) {
        return shapeModelerParameters[ind];
    }

    /**
     * 
     * @param int massDetectorInd
     * @param int chromatogramResolverInd
     * @param int peakResolverInd
     */
    public void setTypeNumber(int shapeModelerInd) {
        myParameters.setParameterValue(shapeModelerTypeNumber, shapeModelerInd);
    }

    /**
     * 
     * @return Integer peakResolverTypeNumber
     */
    public int getPeakModelerTypeNumber() {
        return (Integer) myParameters.getParameterValue(shapeModelerTypeNumber);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {

        for (int i = 0; i < shapeModelerParameters.length; i++) {
            Element subElement = element.addElement("shapemodeler");
            subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
                    shapeModelerNames[i]);
            shapeModelerParameters[i].exportValuesToXML(subElement);
        }

        myParameters.exportValuesToXML(element);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {

        Iterator paramIter = element.elementIterator("shapemodeler");
        while (paramIter.hasNext()) {
            Element paramElem = (Element) paramIter.next();
            for (int i = 0; i < shapeModelerNames.length; i++) {
                if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
                        shapeModelerNames[i])) {
                    shapeModelerParameters[i].importValuesFromXML(paramElem);
                    break;
                }
            }
        }

        myParameters.importValuesFromXML(element);

    }

    /**
     * This function allows to use these parameters by others threads. So it is
     * possible to configure any other task with different parameters value
     * without modify the behavior of other launched tasks
     * 
     */
    public ShapeModelerParameters clone() {

    	ShapeModelerParameters newSet = new ShapeModelerParameters();

        newSet.shapeModelerParameters = new SimpleParameterSet[shapeModelerParameters.length];
        for (int i = 0; i < shapeModelerParameters.length; i++) {
            newSet.shapeModelerParameters[i] = shapeModelerParameters[i].clone();
        }

        newSet.myParameters = myParameters.clone();
        return newSet;

    }

    public Object getParameterValue(Parameter parameter) {
        return myParameters.getParameterValue(parameter);
    }
    
    void setParameterValue(Parameter parameter, Object value) {
        myParameters.setParameterValue(parameter, value);
    }

    public Parameter[] getParameters() {
        return myParameters.getParameters();
    }


}
