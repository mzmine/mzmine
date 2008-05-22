/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.twostep;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.util.Range;

import org.dom4j.Element;

public class TwoStepPickerParameters implements StorableParameterSet {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static final String PARAMETER_ELEMENT_NAME = "TwoStepParameter";
    public static final String PARAMETER_NAME_ATTRIBUTE = "name";
    public static final String PARAMETER_TYPE_ATTRIBUTE = "type";
    
    public static final String massDetectorNames[] = { "Centroid",
            "Local maxima", "Exact mass", "Wavelet transform" };

    public static final String massDetectorClasses[] = {
            "net.sf.mzmine.modules.peakpicking.twostep.massdetection.centroid.CentroidMassDetector",
            "net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima.LocalMaxMassDetector",
            "net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.ExactMassDetector",
            "net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet.WaveletMassDetector" };

    public static final String peakBuilderNames[] = { "Simple data point connector" };

    public static final String peakBuilderClasses[] = { 
            "net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector.SimpleConnector" };

    private SimpleParameterSet massDetectorParameters[];
    private SimpleParameterSet peakBuilderParameters[];

    public TwoStepPickerParameters()  {

    	massDetectorTypeNumber = 0;
    	peakBuilderTypeNumber = 0;
    	
    	massDetectorParameters = new SimpleParameterSet[massDetectorClasses.length];
    	peakBuilderParameters = new SimpleParameterSet[peakBuilderClasses.length];
    	
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

    	for (int i = 0; i < peakBuilderClasses.length; i++) {
    		String className = peakBuilderClasses[i] + "Parameters";
    		Class paramClass;
			try {
				paramClass = Class.forName(className);
				peakBuilderParameters[i] = (SimpleParameterSet) paramClass.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
    
 
    public SimpleParameterSet getMassDetectorParameters(int ind) {
    	return massDetectorParameters[ind];
    }

    public SimpleParameterSet getPeakBuilderParameters(int ind) {
    	return peakBuilderParameters[ind];
    }

    private String suffix= "twosteps";
    
    public void setSuffix (String suffix){
    	this.suffix = suffix;
    }

    public String getSuffix (){
    	return suffix;
    }

    private int massDetectorTypeNumber, peakBuilderTypeNumber;
    
    public void setTypeNumber (int massDetectorInd, int peakBuilderInd){
    	massDetectorTypeNumber = massDetectorInd;
    	peakBuilderTypeNumber = peakBuilderInd;
    }
    
    public int getMassDetectorTypeNumber (){
    	return massDetectorTypeNumber;
    }
    
    public int getPeakBuilderTypeNumber (){
    	return peakBuilderTypeNumber;
    }
        	
    public void exportValuesToXML(Element element) {
        
    	for (int i = 0; i < massDetectorParameters.length; i++) {
        	Element subElement = element.addElement(removeSpaces(massDetectorNames[i]));    		
    		massDetectorParameters[i].exportValuesToXML(subElement);
    	}
    	
    	for (int i = 0; i < peakBuilderParameters.length; i++) {
        	Element subElement = element.addElement(removeSpaces(peakBuilderNames[i]));    		
        	peakBuilderParameters[i].exportValuesToXML(subElement);
    	}

    	Element newElement = element.addElement(PARAMETER_ELEMENT_NAME);
    	Element newElement1 = newElement.addElement("parameter");
    	newElement1.addAttribute(PARAMETER_NAME_ATTRIBUTE, "suffix");
        newElement1.addAttribute(PARAMETER_TYPE_ATTRIBUTE, "STRING");
        newElement1.addText(this.suffix);
        
    	Element newElement2 = newElement.addElement("parameter");
    	newElement2.addAttribute(PARAMETER_NAME_ATTRIBUTE, "massDetectorTypeNumber");
        newElement2.addAttribute(PARAMETER_TYPE_ATTRIBUTE, "INTEGER");
        newElement2.addText(Integer.toString(this.massDetectorTypeNumber));
        
    	Element newElement3 = newElement.addElement("parameter");
    	newElement3.addAttribute(PARAMETER_NAME_ATTRIBUTE, "peakBuilderTypeNumber");
        newElement3.addAttribute(PARAMETER_TYPE_ATTRIBUTE, "INTEGER");
        newElement3.addText(Integer.toString(this.peakBuilderTypeNumber));
        
    }

    public void importValuesFromXML(Element element) {
        // TODO Auto-generated method stub
        
    	for (int i = 0; i < massDetectorParameters.length; i++) {
    		Element paramElem = element.element(removeSpaces(massDetectorNames[i]));
   			massDetectorParameters[i].importValuesFromXML(paramElem);
    	}
    	for (int i = 0; i < peakBuilderParameters.length; i++) {
    		Element paramElem = element.element(removeSpaces(peakBuilderNames[i]));
    		peakBuilderParameters[i].importValuesFromXML(paramElem);
    	}
    	
    	Element paramElem = element.element(PARAMETER_ELEMENT_NAME);
    	Iterator paramIter = paramElem.elementIterator("parameter");
		while (paramIter.hasNext()) {
			Element paramElemTwoStep = (Element) paramIter.next();
			String valueText = paramElemTwoStep.getText();
    		if (paramElemTwoStep.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals("suffix")){
    			this.suffix = valueText;
    		}
			if (paramElemTwoStep.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals("massDetectorTypeNumber")){
    			this.massDetectorTypeNumber = Integer.parseInt(valueText);
    		}
    		if (paramElemTwoStep.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals("peakBuilderTypeNumber")){
    			this.peakBuilderTypeNumber = Integer.parseInt(valueText);
    		}
    	}
    }

    public TwoStepPickerParameters clone() {
        
            // do not make a new instance of SimpleParameterSet, but instead
            // clone the runtime class of this instance - runtime type may be
            // inherited class
        	TwoStepPickerParameters newSet = new TwoStepPickerParameters(); //this.getClass().newInstance();
            newSet.massDetectorParameters = new SimpleParameterSet[massDetectorParameters.length];
            for (int i = 0; i < massDetectorParameters.length; i++){
            	newSet.massDetectorParameters[i] = massDetectorParameters[i].clone();
            }
            newSet.peakBuilderParameters = new SimpleParameterSet[peakBuilderParameters.length];
            for (int i = 0; i < peakBuilderParameters.length; i++){
            	newSet.peakBuilderParameters[i] = peakBuilderParameters[i].clone();
            }
            newSet.suffix = this.suffix;
            newSet.massDetectorTypeNumber = getMassDetectorTypeNumber ();
            newSet.peakBuilderTypeNumber = getPeakBuilderTypeNumber ();
            return newSet;

    }
    
    private String removeSpaces(String s) {
    
    	StringTokenizer st = new StringTokenizer(s," ",false);
    	String t="";
    	while (st.hasMoreElements()) t += st.nextElement();
    	return t;
    }

}
