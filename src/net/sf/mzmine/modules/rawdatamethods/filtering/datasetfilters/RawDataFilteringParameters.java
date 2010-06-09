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
package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class RawDataFilteringParameters implements StorableParameterSet {

	private static final String PARAMETER_NAME_ATTRIBUTE = "name";

	// Raw Data Filters
	public static final String rawDataFilterNames[] = {"Test"};
	public static final String rawDataFilterClasses[] = {
		"net.sf.mzmine.modules.rawdata.datasetfilters.rtcorrection.RTCorrectionFilter"};

	// I have to create the help file for every filter...
	public static final String rawDataFilterHelpFiles[] = {
		"net/sf/mzmine/modules/rawdata/scanfilters/savitzkygolay/help/SGFilter.html"};

	// All parameters
	private SimpleParameterSet rawDataFilterParameters[],  myParameters;
	private static final Parameter rawDataFilterTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Raw Data Filter type",
			"This value defines the type of prefilter for the raw data",
			0);
	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix", "filtered");
	public static final Parameter autoRemove = new SimpleParameter(
			ParameterType.BOOLEAN,
			"Remove source file after filtering",
			"If checked, original file will be removed and only filtered version remains",
			new Boolean(true));

	public RawDataFilteringParameters() {

		rawDataFilterParameters = new SimpleParameterSet[rawDataFilterClasses.length];
		myParameters = new SimpleParameterSet(new Parameter[]{
					rawDataFilterTypeNumber, suffix, autoRemove});
		for (int i = 0; i < rawDataFilterClasses.length; i++) {
			String className = rawDataFilterClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				rawDataFilterParameters[i] = (SimpleParameterSet) paramClass.newInstance();
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
	public SimpleParameterSet getRawDataFilteringParameters(int ind) {
		return rawDataFilterParameters[ind];
	}

	/**
	 * 
	 * @param String
	 *            title
	 */
	public void setSuffix(String title) {
		if (title.equals("")) {
			title = "filtered";
		}
		myParameters.setParameterValue(suffix, title);
	}

	/**
	 * 
	 * @return String
	 */
	public String getSuffix() {
		String Suffix = (String) myParameters.getParameterValue(suffix);
		if (Suffix == null) {
			return "filtered";
		}
		if (Suffix.equals("")) {
			return "filtered";
		}
		return Suffix;
	}

	/**
	 *
	 * @param boolena value
	 */
	public void setAutoRemove(boolean value) {
		myParameters.setParameterValue(autoRemove, value);
	}

	/**
	 *
	 * @return boolean
	 */
	public boolean getAutoRemove() {
		return (Boolean) myParameters.getParameterValue(autoRemove);
	}

	/**
	 * 
	 * @param int rawDataFilterInd
	 */
	public void setTypeNumber(int rawDataFilterInd) {
		myParameters.setParameterValue(rawDataFilterTypeNumber, rawDataFilterInd);
	}

	/**
	 * 
	 * @return Integer rawDataFilterTypeNumber
	 */
	public int getRawDataFilterTypeNumber() {
		return (Integer) myParameters.getParameterValue(rawDataFilterTypeNumber);
	}

	/**
	 * 
	 * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
	 */
	public void exportValuesToXML(Element element) {

		for (int i = 0; i < rawDataFilterParameters.length; i++) {
			Element subElement = element.addElement("rawdatafilter");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
					rawDataFilterNames[i]);
			rawDataFilterParameters[i].exportValuesToXML(subElement);
		}
		myParameters.exportValuesToXML(element);
	}

	/**
	 * 
	 * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
	 */
	public void importValuesFromXML(Element element) {

		Iterator paramIter = element.elementIterator("rawdatafilter");
		while (paramIter.hasNext()) {
			Element paramElem = (Element) paramIter.next();
			for (int i = 0; i < rawDataFilterNames.length; i++) {
				if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
						rawDataFilterNames[i])) {
					rawDataFilterParameters[i].importValuesFromXML(paramElem);
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
	public RawDataFilteringParameters clone() {

		RawDataFilteringParameters newSet = new RawDataFilteringParameters();

		newSet.rawDataFilterParameters = new SimpleParameterSet[rawDataFilterParameters.length];
		for (int i = 0; i < rawDataFilterParameters.length; i++) {
			newSet.rawDataFilterParameters[i] = rawDataFilterParameters[i].clone();
		}
		newSet.myParameters = myParameters.clone();
		return newSet;

	}

	public Parameter[] getParameters() {
		return myParameters.getParameters();
	}
}
