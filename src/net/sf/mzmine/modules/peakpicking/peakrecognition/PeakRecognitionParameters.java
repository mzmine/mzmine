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

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class PeakRecognitionParameters implements StorableParameterSet {

	private static final String PARAMETER_NAME_ATTRIBUTE = "name";

	// Peak recognition
	public static final String peakResolverNames[] = { "Baseline cut-off",
			"Chromatographic threshold", "Noise amplitude",
			"Standard deviation", "Savitzky-Golay", "Wavelet transform",
			"Local minimum search" };

	public static final String peakResolverClasses[] = {
			"net.sf.mzmine.modules.peakpicking.peakrecognition.baseline.BaselinePeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.chromatographicthreshold.ChromatographicThresholdPeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.noiseamplitude.NoiseAmplitudePeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.standarddeviation.StandardDeviationPeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.savitzkygolay.SavitzkyGolayPeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.wavelet.WaveletPeakDetector",
			"net.sf.mzmine.modules.peakpicking.peakrecognition.minimumsearch.MinimumSearch", };

	public static final String peakResolverHelpFiles[] = {
			"net/sf/mzmine/modules/peakpicking/peakrecognition/baseline/help/BaselinePeakDetector.html",
			"net/sf/mzmine/modules/peakpicking/peakrecognition/chromatographicthreshold/help/ChromatographicThresholdPeakDetector.html",
			"net/sf/mzmine/modules/peakpicking/peakrecognition/noiseamplitude/help/NoiseAmplitudePeakDetector.html",
			"net/sf/mzmine/modules/peakpicking/peakrecognition/standarddeviation/help/StandardDeviationPeakDetector.html",
			"net/sf/mzmine/modules/peakpicking/peakrecognition/savitzkygolay/help/SavitzkyGolayPeakDetector.html",
			"net/sf/mzmine/modules/peakpicking/peakrecognition/wavelet/help/WaveletPeakDetector.html",
			"TODO" };

	// Three step parameters
	private SimpleParameterSet peakResolverParameters[], myParameters;

	private static final Parameter peakResolverTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Peak Builder type",
			"This value defines the type of peak builder to use in three steps peak picking process",
			0);

	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix", "resolved");

	public PeakRecognitionParameters() {

		peakResolverParameters = new SimpleParameterSet[peakResolverClasses.length];

		myParameters = new SimpleParameterSet(new Parameter[] {

		peakResolverTypeNumber, suffix });

		for (int i = 0; i < peakResolverClasses.length; i++) {
			String className = peakResolverClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				peakResolverParameters[i] = (SimpleParameterSet) paramClass
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
	public SimpleParameterSet getPeakBuilderParameters(int ind) {
		return peakResolverParameters[ind];
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
	 * @param int chromatogramBuilderInd
	 * @param int peakResolverInd
	 */
	public void setTypeNumber(int peakResolverInd) {

		myParameters.setParameterValue(peakResolverTypeNumber, peakResolverInd);
	}

	/**
	 * 
	 * @return Integer peakResolverTypeNumber
	 */
	public int getPeakBuilderTypeNumber() {
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
	public PeakRecognitionParameters clone() {

		PeakRecognitionParameters newSet = new PeakRecognitionParameters();

		newSet.peakResolverParameters = new SimpleParameterSet[peakResolverParameters.length];
		for (int i = 0; i < peakResolverParameters.length; i++) {
			newSet.peakResolverParameters[i] = peakResolverParameters[i].clone();
		}

		newSet.myParameters = myParameters.clone();
		return newSet;

	}

	public Object getParameterValue(Parameter parameter) {
		Object objectValue = myParameters.getParameterValue(parameter);
		if (objectValue instanceof String)
			return objectValue;

		int index = (Integer) objectValue;
		String parameterName = parameter.getName();

		if (parameterName.equals("Peak Builder type")) {
			return peakResolverNames[index];
		}
		return null;
	}

	public Parameter[] getParameters() {
		return myParameters.getParameters();
	}

}
