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
			"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.centroid.CentroidMassDetector",
			"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.ExactMassDetector",
			"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.localmaxima.LocalMaxMassDetector",
			"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.recursive.RecursiveMassDetector",
			"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.wavelet.WaveletMassDetector" };

	public static final String massDetectorHelpFiles[] = {
			"net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massdetection/centroid/help/CentroidMassDetector.html",
			"net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massdetection/exactmass/help/ExactMassDetector.html",
			"net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massdetection/localmaxima/help/LocalMaxMassDetector.html",
			"net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massdetection/recursive/help/RecursiveMassDetector.html",
			"net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massdetection/wavelet/help/WaveletMassDetector.html" };

	// Chromatogram Builders
	public static final String massConnectorNames[] = { "Highest data point" };

	public static final String massConnectorClasses[] = { "net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.highestdatapoint.HighestDataPointConnector" };

	public static final String massConnectorHelpFiles[] = { "net/sf/mzmine/modules/peakpicking/chromatogrambuilder/massconnection/highestdatapoint/help/HighestDatapointConnector.html" };

	// All parameters
	private SimpleParameterSet massDetectorParameters[],
			massConnectorParameters[], myParameters;

	private static final Parameter massDetectorTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Mass Detector type",
			"This value defines the type of mass detector to use in three steps peak picking process",
			0);

	private static final Parameter massConnectorTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Chromatogram Builder type",
			"This value defines the type of chromatogram builder to use in three steps peak picking process",
			0);

	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix", "peaklist");

	public ChromatogramBuilderParameters() {

		massDetectorParameters = new SimpleParameterSet[massDetectorClasses.length];
		massConnectorParameters = new SimpleParameterSet[massConnectorClasses.length];

		myParameters = new SimpleParameterSet(new Parameter[] {
				massDetectorTypeNumber, massConnectorTypeNumber, suffix });

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

		for (int i = 0; i < massConnectorClasses.length; i++) {
			String className = massConnectorClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				massConnectorParameters[i] = (SimpleParameterSet) paramClass
						.newInstance();
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
	public SimpleParameterSet getMassConnectorParameters(int ind) {
		return massConnectorParameters[ind];
	}

	/**
	 * 
	 * @param String
	 *            title
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
	 * @param int massConnectorInd
	 * @param int peakBuilderInd
	 */
	public void setTypeNumber(int massDetectorInd, int massConnectorInd) {
		myParameters.setParameterValue(massDetectorTypeNumber, massDetectorInd);
		myParameters.setParameterValue(massConnectorTypeNumber,
				massConnectorInd);
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
	 * @return Integer massConnectorTypeNumber
	 */
	public int getMassConnectorTypeNumber() {
		return (Integer) myParameters
				.getParameterValue(massConnectorTypeNumber);
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

		for (int i = 0; i < massConnectorParameters.length; i++) {
			Element subElement = element.addElement("massconnector");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
					massConnectorNames[i]);
			massConnectorParameters[i].exportValuesToXML(subElement);
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

		paramIter = element.elementIterator("massconnector");
		while (paramIter.hasNext()) {
			Element paramElem = (Element) paramIter.next();
			for (int i = 0; i < massConnectorNames.length; i++) {
				if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
						massConnectorNames[i])) {
					massConnectorParameters[i].importValuesFromXML(paramElem);
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
			newSet.massDetectorParameters[i] = massDetectorParameters[i]
					.clone();
		}

		newSet.massConnectorParameters = new SimpleParameterSet[massConnectorParameters.length];
		for (int i = 0; i < massConnectorParameters.length; i++) {
			newSet.massConnectorParameters[i] = massConnectorParameters[i]
					.clone();
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
			return massConnectorNames[index];
		}
		return null;
	}

	public Parameter[] getParameters() {
		return myParameters.getParameters();
	}

}
