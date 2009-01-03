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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class ChromatogramBuilderParameters implements StorableParameterSet {

    private static final String PARAMETER_NAME_ATTRIBUTE = "name";

    // Mass Detectors
    public static final String massDetectorNames[] = { "Centroid",
            "Exact mass", "Local maxima", "Recursive threshold",
            "Wavelet transform" };

    public static final String massDetectorClasses[] = {
            "net.sf.mzmine.modules.peakpicking.threestep.massdetection.centroid.CentroidMassDetector",
            "net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass.ExactMassDetector",
            "net.sf.mzmine.modules.peakpicking.threestep.massdetection.localmaxima.LocalMaxMassDetector",
            "net.sf.mzmine.modules.peakpicking.threestep.massdetection.recursive.RecursiveMassDetector",
            "net.sf.mzmine.modules.peakpicking.threestep.massdetection.wavelet.WaveletMassDetector" };

    public static final String massDetectorHelpFiles[] = {
            "net/sf/mzmine/modules/peakpicking/threestep/massdetection/centroid/help/CentroidMassDetector.html",
            "net/sf/mzmine/modules/peakpicking/threestep/massdetection/exactmass/help/ExactMassDetector.html",
            "net/sf/mzmine/modules/peakpicking/threestep/massdetection/localmaxima/help/LocalMaxMassDetector.html",
            "net/sf/mzmine/modules/peakpicking/threestep/massdetection/recursive/help/RecursiveMassDetector.html",
            "net/sf/mzmine/modules/peakpicking/threestep/massdetection/wavelet/help/WaveletMassDetector.html" };

    // Chromatogram Builders
    public static final String chromatogramBuilderNames[] = {
            "Score connector", "Highest datapoint" };

    public static final String chromatogramBuilderClasses[] = {
            "net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.scoreconnector.ScoreConnector",
            "net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.highestdatapoint.HighestDatapointConnector" };

    public static final String chromatogramBuilderHelpFiles[] = {
            "net/sf/mzmine/modules/peakpicking/threestep/xicconstruction/scoreconnector/help/SimpleConnector.html",
            "net/sf/mzmine/modules/peakpicking/threestep/xicconstruction/highestdatapoint/help/HighIntensityConnector.html" };

    // Three step parameters
    private SimpleParameterSet massDetectorParameters[],
            chromatogramBuilderParameters[], myParameters;

    private static final Parameter massDetectorTypeNumber = new SimpleParameter(
            ParameterType.INTEGER,
            "Mass Detector type",
            "This value defines the type of mass detector to use in three steps peak picking process",
            0);

    private static final Parameter chromatogramBuilderTypeNumber = new SimpleParameter(
            ParameterType.INTEGER,
            "Chromatogram Builder type",
            "This value defines the type of chromatogram builder to use in three steps peak picking process",
            0);

    private static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Suffix",
            "This string is added to filename as suffix", "peaklist");

    public ChromatogramBuilderParameters() {

        massDetectorParameters = new SimpleParameterSet[massDetectorClasses.length];
        chromatogramBuilderParameters = new SimpleParameterSet[chromatogramBuilderClasses.length];

        myParameters = new SimpleParameterSet(new Parameter[] {
                massDetectorTypeNumber, chromatogramBuilderTypeNumber, suffix });

        for (int i = 0; i < massDetectorClasses.length; i++) {
            String className = massDetectorClasses[i] + "Parameters";
            Class paramClass;
            try {
                paramClass = Class.forName(className);
                massDetectorParameters[i] = (SimpleParameterSet) paramClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < chromatogramBuilderClasses.length; i++) {
            String className = chromatogramBuilderClasses[i] + "Parameters";
            Class paramClass;
            try {
                paramClass = Class.forName(className);
                chromatogramBuilderParameters[i] = (SimpleParameterSet) paramClass.newInstance();
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
    public SimpleParameterSet getMassDetectorParameters(int ind) {
        return massDetectorParameters[ind];
    }

    /**
     * 
     * @param int index
     * @return SimpleParameterSet
     */
    public SimpleParameterSet getChromatogramBuilderParameters(int ind) {
        return chromatogramBuilderParameters[ind];
    }

    /**
     * 
     * @param String title
     */
    public void setSuffix(String title) {
        if (title.equals(""))
            title = "peakList";
        myParameters.setParameterValue(suffix, title);
    }

    /**
     * 
     * @return String
     */
    public String getSuffix() {
        String Suffix = (String) myParameters.getParameterValue(suffix);
        if (Suffix == null)
            return "peaklist";
        if (Suffix.equals(""))
            return "peaklist";
        return Suffix;
    }

    /**
     * 
     * @param int massDetectorInd
     * @param int chromatogramBuilderInd
     * @param int peakBuilderInd
     */
    public void setTypeNumber(int massDetectorInd, int chromatogramBuilderInd) {
        myParameters.setParameterValue(massDetectorTypeNumber, massDetectorInd);
        myParameters.setParameterValue(chromatogramBuilderTypeNumber,
                chromatogramBuilderInd);
    }

    /**
     * 
     * @return Integer massDetectorTypeNumber
     */
    public int getMassDetectorTypeNumber() {
        return (Integer) myParameters.getParameterValue(massDetectorTypeNumber);
    }

    /**
     * 
     * @return Integer chromatogramBuilderTypeNumber
     */
    public int getChromatogramBuilderTypeNumber() {
        return (Integer) myParameters.getParameterValue(chromatogramBuilderTypeNumber);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {

        for (int i = 0; i < massDetectorParameters.length; i++) {
            Element subElement = element.addElement("massdetector");
            subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
                    massDetectorNames[i]);
            massDetectorParameters[i].exportValuesToXML(subElement);
        }

        for (int i = 0; i < chromatogramBuilderParameters.length; i++) {
            Element subElement = element.addElement("chromatogrambuilder");
            subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
                    chromatogramBuilderNames[i]);
            chromatogramBuilderParameters[i].exportValuesToXML(subElement);
        }

        myParameters.exportValuesToXML(element);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {

        Iterator paramIter = element.elementIterator("massdetector");
        while (paramIter.hasNext()) {
            Element paramElem = (Element) paramIter.next();
            for (int i = 0; i < massDetectorNames.length; i++) {
                if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
                        massDetectorNames[i])) {
                    massDetectorParameters[i].importValuesFromXML(paramElem);
                    break;
                }
            }
        }

        Iterator paramIter2 = element.elementIterator("chromatogrambuilder");
        while (paramIter2.hasNext()) {
            Element paramElem = (Element) paramIter2.next();
            for (int i = 0; i < chromatogramBuilderNames.length; i++) {
                if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
                        chromatogramBuilderNames[i])) {
                    chromatogramBuilderParameters[i].importValuesFromXML(paramElem);
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
    public ChromatogramBuilderParameters clone() {

        ChromatogramBuilderParameters newSet = new ChromatogramBuilderParameters();

        newSet.massDetectorParameters = new SimpleParameterSet[massDetectorParameters.length];
        for (int i = 0; i < massDetectorParameters.length; i++) {
            newSet.massDetectorParameters[i] = massDetectorParameters[i].clone();
        }

        newSet.chromatogramBuilderParameters = new SimpleParameterSet[chromatogramBuilderParameters.length];
        for (int i = 0; i < chromatogramBuilderParameters.length; i++) {
            newSet.chromatogramBuilderParameters[i] = chromatogramBuilderParameters[i].clone();
        }

        newSet.myParameters = myParameters.clone();
        return newSet;

    }

    public Object getParameterValue(Parameter parameter) {
        Object objectValue = myParameters.getParameterValue(parameter);
        if (objectValue instanceof String)
            return objectValue.toString();

        int index = (Integer) objectValue;
        String parameterName = parameter.getName();
        if (parameterName.equals("Mass Detector type")) {
            return massDetectorNames[index];
        }
        if (parameterName.equals("Chromatogram Builder type")) {
            return chromatogramBuilderNames[index];
        }
        return null;
    }

    public Parameter[] getParameters() {
        return myParameters.getParameters();
    }

}
