/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.deconvolution;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class DeconvolutionParameters implements StorableParameterSet {

    private static final String PARAMETER_NAME_ATTRIBUTE = "name";

    // Peak recognition
    public static final String peakResolverNames[] = { "Baseline cut-off",
            "Noise amplitude", "Savitzky-Golay",
            "Local minimum search" };

    public static final String peakResolverClasses[] = {
            "net.sf.mzmine.modules.peakpicking.deconvolution.baseline.BaselinePeakDetector",
            "net.sf.mzmine.modules.peakpicking.deconvolution.noiseamplitude.NoiseAmplitudePeakDetector",
            "net.sf.mzmine.modules.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetector",
            "net.sf.mzmine.modules.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetector" };

    public static final String peakResolverHelpFiles[] = {
            "net/sf/mzmine/modules/peakpicking/deconvolution/baseline/help/BaselinePeakDetector.html",
            "net/sf/mzmine/modules/peakpicking/deconvolution/noiseamplitude/help/NoiseAmplitudePeakDetector.html",
            "net/sf/mzmine/modules/peakpicking/deconvolution/savitzkygolay/help/SavitzkyGolayPeakDetector.html",
            "TODO" };

    // Three step parameters
    private SimpleParameterSet peakResolverParameters[], myParameters;

    private static final Parameter peakResolverTypeNumber = new SimpleParameter(
            ParameterType.INTEGER,
            "Peak Resolver type",
            "This value defines the type of peak builder to use in three steps peak picking process",
            0);

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Suffix",
            "This string is added to filename as suffix", "resolved");

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only resolved version remains",
            new Boolean(false));

    public DeconvolutionParameters() {

        peakResolverParameters = new SimpleParameterSet[peakResolverClasses.length];

        myParameters = new SimpleParameterSet(new Parameter[] {
                peakResolverTypeNumber, suffix, autoRemove });

        for (int i = 0; i < peakResolverClasses.length; i++) {
            String className = peakResolverClasses[i] + "Parameters";
            Class paramClass;
            try {
                paramClass = Class.forName(className);
                peakResolverParameters[i] = (SimpleParameterSet) paramClass.newInstance();
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
    public SimpleParameterSet getPeakResolverParameters(int ind) {
        return peakResolverParameters[ind];
    }

    /**
     * 
     * @param int massDetectorInd
     * @param int chromatogramResolverInd
     * @param int peakResolverInd
     */
    public void setTypeNumber(int peakResolverInd) {
        myParameters.setParameterValue(peakResolverTypeNumber, peakResolverInd);
    }

    /**
     * 
     * @return Integer peakResolverTypeNumber
     */
    public int getPeakResolverTypeNumber() {
        return (Integer) myParameters.getParameterValue(peakResolverTypeNumber);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {

        for (int i = 0; i < peakResolverParameters.length; i++) {
            Element subElement = element.addElement("peakbuilder");
            subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
                    peakResolverNames[i]);
            peakResolverParameters[i].exportValuesToXML(subElement);
        }

        myParameters.exportValuesToXML(element);
    }

    /**
     * 
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {

        Iterator paramIter = element.elementIterator("peakbuilder");
        while (paramIter.hasNext()) {
            Element paramElem = (Element) paramIter.next();
            for (int i = 0; i < peakResolverNames.length; i++) {
                if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
                        peakResolverNames[i])) {
                    peakResolverParameters[i].importValuesFromXML(paramElem);
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
    public DeconvolutionParameters clone() {

        DeconvolutionParameters newSet = new DeconvolutionParameters();

        newSet.peakResolverParameters = new SimpleParameterSet[peakResolverParameters.length];
        for (int i = 0; i < peakResolverParameters.length; i++) {
            newSet.peakResolverParameters[i] = peakResolverParameters[i].clone();
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
