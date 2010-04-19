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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.MassConnector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.highestdatapoint.HighestDataPointConnector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.localmaxima.LocalMaxMassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.recursive.RecursiveMassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.wavelet.WaveletMassDetector;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massfilters.MassFilter;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massfilters.shoulderpeaksfilter.ShoulderPeaksFilter;
import net.sf.mzmine.util.ExceptionUtils;

import org.dom4j.Element;

public class ChromatogramBuilderParameters implements StorableParameterSet {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String PARAMETER_NAME_ATTRIBUTE = "name";

	private static final Class massDetectorClasses[] = {
			CentroidMassDetector.class, ExactMassDetector.class,
			LocalMaxMassDetector.class, RecursiveMassDetector.class,
			WaveletMassDetector.class };

	private static final Class massFilterClasses[] = { ShoulderPeaksFilter.class };

	private static final Class massConnectorClasses[] = { HighestDataPointConnector.class };

	// All parameters
	private SimpleParameterSet myParameters;

	private static final Parameter massDetectorTypeNumber = new SimpleParameter(
			ParameterType.INTEGER, "Mass detector type", null, 0);

	private static final Parameter massFilterTypeNumber = new SimpleParameter(
			ParameterType.INTEGER, "Filter type", null, 0);

	private static final Parameter massConnectorTypeNumber = new SimpleParameter(
			ParameterType.INTEGER, "Mass connector type", null, 0);

	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix", "peaklist");

	private MassDetector massDetectors[];
	private MassFilter massFilters[];
	private MassConnector massConnectors[];

	public ChromatogramBuilderParameters() {

		myParameters = new SimpleParameterSet(new Parameter[] {
				massDetectorTypeNumber, massFilterTypeNumber,
				massConnectorTypeNumber, suffix });

		ArrayList<Object> newInstances = new ArrayList<Object>();

		// Create an instance of each mass detector
		for (int i = 0; i < massDetectorClasses.length; i++) {
			try {
				Object newInstance = massDetectorClasses[i].newInstance();
				newInstances.add(newInstance);
			} catch (Exception e) {
				String message = "Could not create instance of class "
						+ massDetectorClasses[i] + ": "
						+ ExceptionUtils.exceptionToString(e);
				logger.warning(message);
			}
		}
		massDetectors = newInstances.toArray(new MassDetector[0]);
		newInstances.clear();

		// Create an instance of each mass filter
		for (int i = 0; i < massFilterClasses.length; i++) {
			try {
				Object newInstance = massFilterClasses[i].newInstance();
				newInstances.add(newInstance);
			} catch (Exception e) {
				String message = "Could not create instance of class "
						+ massFilterClasses[i] + ": "
						+ ExceptionUtils.exceptionToString(e);
				logger.warning(message);
			}
		}
		massFilters = newInstances.toArray(new MassFilter[0]);
		newInstances.clear();

		// Create an instance of each mass connector
		for (int i = 0; i < massConnectorClasses.length; i++) {
			try {
				Object newInstance = massConnectorClasses[i].newInstance();
				newInstances.add(newInstance);
			} catch (Exception e) {
				String message = "Could not create instance of class "
						+ massConnectorClasses[i] + ": "
						+ ExceptionUtils.exceptionToString(e);
				logger.warning(message);
			}
		}
		massConnectors = newInstances.toArray(new MassConnector[0]);

	}

	/**
	 * 
	 * @param String
	 *            title
	 */
	public void setSuffix(String title) {
		myParameters.setParameterValue(suffix, title);
	}

	/**
	 * 
	 * @return String
	 */
	public String getSuffix() {
		String suffixValue = (String) myParameters.getParameterValue(suffix);
		if ((suffixValue == null) || (suffixValue.equals("")))
			return "peak list";
		return suffixValue;
	}

	/**
	 * 
	 * @param int massDetectorInd
	 * @param int massConnectorInd
	 * @param int peakBuilderInd
	 */
	public void setTypeNumber(int massDetectorInd, int massFilterInd,
			int massConnectorInd) {
		myParameters.setParameterValue(massDetectorTypeNumber, massDetectorInd);
		myParameters.setParameterValue(massFilterTypeNumber, massFilterInd);
		myParameters.setParameterValue(massConnectorTypeNumber,
				massConnectorInd);
	}

	/**
	 * 
	 * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
	 */
	public void exportValuesToXML(Element element) {

		for (int i = 0; i < massDetectors.length; i++) {
			Element subElement = element.addElement("massdetector");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE, massDetectors[i]
					.getName());
			SimpleParameterSet params = massDetectors[i].getParameters();
			params.exportValuesToXML(subElement);
		}

		for (int i = 0; i < massFilters.length; i++) {
			Element subElement = element.addElement("massfilter");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE, massFilters[i]
					.getName());
			SimpleParameterSet params = massFilters[i].getParameters();
			params.exportValuesToXML(subElement);
		}

		for (int i = 0; i < massConnectors.length; i++) {
			Element subElement = element.addElement("massconnector");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE, massConnectors[i]
					.getName());
			SimpleParameterSet params = massConnectors[i].getParameters();
			params.exportValuesToXML(subElement);
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
			String name = paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE);
			for (int i = 0; i < massDetectors.length; i++) {
				if (name.equals(massDetectors[i].getName())) {
					SimpleParameterSet params = massDetectors[i]
							.getParameters();
					params.importValuesFromXML(paramElem);
					break;
				}
			}
		}

		paramIter = element.elementIterator("massfilter");
		while (paramIter.hasNext()) {
			Element paramElem = (Element) paramIter.next();
			for (int i = 0; i < massFilters.length; i++) {
				String name = paramElem
						.attributeValue(PARAMETER_NAME_ATTRIBUTE);
				if (name.equals(massFilters[i].getName())) {
					SimpleParameterSet params = massFilters[i].getParameters();
					params.importValuesFromXML(paramElem);
					break;
				}
			}
		}

		paramIter = element.elementIterator("massconnector");
		while (paramIter.hasNext()) {
			Element paramElem = (Element) paramIter.next();
			for (int i = 0; i < massConnectors.length; i++) {
				String name = paramElem
						.attributeValue(PARAMETER_NAME_ATTRIBUTE);

				if (name.equals(massConnectors[i].getName())) {
					SimpleParameterSet params = massConnectors[i]
							.getParameters();
					params.importValuesFromXML(paramElem);
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

		for (int i = 0; i < massDetectors.length; i++) {
			SimpleParameterSet currentParam = massDetectors[i].getParameters();
			SimpleParameterSet newParam = newSet.massDetectors[i]
					.getParameters();
			newParam.importValuesFrom(currentParam);
		}

		for (int i = 0; i < massFilters.length; i++) {
			SimpleParameterSet currentParam = massFilters[i].getParameters();
			SimpleParameterSet newParam = newSet.massFilters[i].getParameters();
			newParam.importValuesFrom(currentParam);
		}

		for (int i = 0; i < massConnectors.length; i++) {
			SimpleParameterSet currentParam = massConnectors[i].getParameters();
			SimpleParameterSet newParam = newSet.massConnectors[i]
					.getParameters();
			newParam.importValuesFrom(currentParam);
		}

		newSet.myParameters = myParameters.clone();

		return newSet;

	}

	public MassDetector[] getMassDetectors() {
		return massDetectors;
	}

	public MassFilter[] getMassFilters() {
		return massFilters;
	}

	public MassConnector[] getMassConnectors() {
		return massConnectors;
	}

	public MassDetector getMassDetector() {
		int massDetectorIndex = (Integer) myParameters
				.getParameterValue(massDetectorTypeNumber);
		if (massDetectorIndex >= massDetectors.length)
			massDetectorIndex = 0;
		return massDetectors[massDetectorIndex];
	}

	public MassFilter getMassFilter() {
		int massFilterIndex = (Integer) myParameters
				.getParameterValue(massFilterTypeNumber);
		if (massFilterIndex < 0)
			return null;
		if (massFilterIndex >= massFilters.length)
			massFilterIndex = 0;
		return massFilters[massFilterIndex];
	}

	public MassConnector getMassConnector() {
		int massConnectorIndex = (Integer) myParameters
				.getParameterValue(massConnectorTypeNumber);
		if (massConnectorIndex >= massDetectors.length)
			massConnectorIndex = 0;
		return massConnectors[massConnectorIndex];
	}

}
