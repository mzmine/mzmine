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

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class TwoStepPickerParameters implements StorableParameterSet {

	private static final String PARAMETER_NAME_ATTRIBUTE = "name";

	public static final String massDetectorNames[] = { "Centroid", "Local maxima",
			"Recursive", "Exact mass", "Wavelet transform" };

	public static final String massDetectorClasses[] = {
			"net.sf.mzmine.modules.peakpicking.twostep.massdetection.centroid.CentroidMassDetector",
			"net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima.LocalMaxMassDetector",
			"net.sf.mzmine.modules.peakpicking.twostep.massdetection.recursive.RecursiveMassDetector",
			"net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.ExactMassDetector",
			"net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet.WaveletMassDetector" };

	public static final String peakBuilderNames[] = { "Simple data point connector" };

	public static final String peakBuilderClasses[] = { "net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector.SimpleConnector" };

	private SimpleParameterSet massDetectorParameters[],
			peakBuilderParameters[], twoStepsParameters;

	private static final Parameter massDetectorTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Mass Detector type",
			"This value defines the type of mass detector to use in two steps peak picking process",
			0);

	private static final Parameter peakBuilderTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Peak Builder type",
			"This value defines the type of peak builder to use in two steps peak picking process",
			0);

	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix",
			new String("peaklist"));

	public TwoStepPickerParameters() {

		massDetectorParameters = new SimpleParameterSet[massDetectorClasses.length];
		peakBuilderParameters = new SimpleParameterSet[peakBuilderClasses.length];

		twoStepsParameters = new SimpleParameterSet(new Parameter[] {
				massDetectorTypeNumber, peakBuilderTypeNumber, suffix });

		for (int i = 0; i < massDetectorClasses.length; i++) {
			String className = massDetectorClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				massDetectorParameters[i] = (SimpleParameterSet) paramClass
						.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (int i = 0; i < peakBuilderClasses.length; i++) {
			String className = peakBuilderClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				peakBuilderParameters[i] = (SimpleParameterSet) paramClass
						.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param int
	 *            index
	 * @return SimpleParameterSet
	 */
	public SimpleParameterSet getMassDetectorParameters(int ind) {
		return massDetectorParameters[ind];
	}

	/**
	 * 
	 * @param int
	 *            index
	 * @return SimpleParameterSet
	 */
	public SimpleParameterSet getPeakBuilderParameters(int ind) {
		return peakBuilderParameters[ind];
	}

	/**
	 * 
	 * @param String
	 *            title
	 */
	public void setSuffix(String title) {
		if (title.equals(""))
			title = "peakList";
		twoStepsParameters.setParameterValue(suffix, title);
	}

	/**
	 * 
	 * @return String
	 */
	public String getSuffix() {
		String Suffix = (String) twoStepsParameters.getParameterValue(suffix);
		if (Suffix == null)
			return "peaklist";
		if (Suffix.equals(""))
			return "peaklist";
		return Suffix;
	}

	/**
	 * 
	 * @param int
	 *            massDetectorInd
	 * @param int
	 *            peakBuilderInd
	 */
	public void setTypeNumber(int massDetectorInd, int peakBuilderInd) {
		twoStepsParameters.setParameterValue(massDetectorTypeNumber,
				massDetectorInd);
		twoStepsParameters.setParameterValue(peakBuilderTypeNumber,
				peakBuilderInd);
	}

	/**
	 * 
	 * @return Integer massDetectorTypeNumber
	 */
	public int getMassDetectorTypeNumber() {
		return (Integer) twoStepsParameters
				.getParameterValue(massDetectorTypeNumber);
	}

	/**
	 * 
	 * @return Integer peakBuilderTypeNumber
	 */
	public int getPeakBuilderTypeNumber() {
		return (Integer) twoStepsParameters
				.getParameterValue(peakBuilderTypeNumber);
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

		for (int i = 0; i < peakBuilderParameters.length; i++) {
			Element subElement = element.addElement("peakbuilder");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
					peakBuilderNames[i]);
			peakBuilderParameters[i].exportValuesToXML(subElement);
		}

		twoStepsParameters.exportValuesToXML(element);
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

		Iterator paramIter2 = element.elementIterator("peakbuilder");
		while (paramIter2.hasNext()) {
			Element paramElem = (Element) paramIter2.next();
			for (int i = 0; i < massDetectorNames.length; i++) {
				if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
						peakBuilderNames[i])) {
					peakBuilderParameters[i].importValuesFromXML(paramElem);
					break;
				}
			}
		}

		twoStepsParameters.importValuesFromXML(element);

	}

	/** 
	 * 
	 */
	public TwoStepPickerParameters clone() {

		// do not make a new instance of SimpleParameterSet, but instead
		// clone the runtime class of this instance - runtime type may be
		// inherited class
		TwoStepPickerParameters newSet = new TwoStepPickerParameters(); // this.getClass().newInstance();
		newSet.massDetectorParameters = new SimpleParameterSet[massDetectorParameters.length];
		for (int i = 0; i < massDetectorParameters.length; i++) {
			newSet.massDetectorParameters[i] = massDetectorParameters[i]
					.clone();
		}
		newSet.peakBuilderParameters = new SimpleParameterSet[peakBuilderParameters.length];
		for (int i = 0; i < peakBuilderParameters.length; i++) {
			newSet.peakBuilderParameters[i] = peakBuilderParameters[i].clone();
		}
		newSet.twoStepsParameters = twoStepsParameters.clone();
		return newSet;

	}

}
