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

package net.sf.mzmine.modules.peakpicking.threestep;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class ThreeStepPickerParameters implements StorableParameterSet {

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
	public static final String chromatogramBuilderNames[] = { "Score connector", "Highest datapoint" };

	public static final String chromatogramBuilderClasses[] = { 
			"net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.scoreconnector.ScoreConnector",
			"net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.highestdatapoint.HighestDatapointConnector" };

	public static final String chromatogramBuilderHelpFiles[] = { 
		"net/sf/mzmine/modules/peakpicking/threestep/xicconstruction/scoreconnector/help/ScoreConnector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/xicconstruction/highestdatapoint/help/HighestDatapointConnector.html" };

	// Peak recognition
	public static final String peakBuilderNames[] = { "No recognition", "Baseline cut-off",
			"Chromatographic threshold", "Noise amplitude",
			"Standard deviation", "Savitzky-Golay", "Wavelet transform" };

	public static final String peakBuilderClasses[] = {
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.simple.SimplePeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.baseline.BaselinePeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.chromatographicthreshold.ChromatographicThresholdPeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.noiseamplitude.NoiseAmplitudePeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.standarddeviation.StandardDeviationPeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.savitzkygolay.SavitzkyGolayPeakDetector",
			"net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.wavelet.WaveletPeakDetector" };

	public static final String peakBuilderHelpFiles[] = {
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/simple/help/SimplePeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/baseline/help/BaselinePeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/chromatographicthreshold/help/ChromatographicThresholdPeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/noiseamplitude/help/NoiseAmplitudePeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/standarddeviation/help/StandardDeviationPeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/savitzkygolay/help/SavitzkyGolayPeakDetector.html",
		"net/sf/mzmine/modules/peakpicking/threestep/peakconstruction/wavelet/help/WaveletPeakDetector.html" };

	
	// Three step parameters
	private SimpleParameterSet massDetectorParameters[],
			chromatogramBuilderParameters[], peakBuilderParameters[],
			threeStepsParameters;

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

	private static final Parameter peakBuilderTypeNumber = new SimpleParameter(
			ParameterType.INTEGER,
			"Peak Builder type",
			"This value defines the type of peak builder to use in three steps peak picking process",
			0);

	private static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix",
			new String("peaklist"));

	public ThreeStepPickerParameters() {

		massDetectorParameters = new SimpleParameterSet[massDetectorClasses.length];
		chromatogramBuilderParameters = new SimpleParameterSet[chromatogramBuilderClasses.length];
		peakBuilderParameters = new SimpleParameterSet[peakBuilderClasses.length];

		threeStepsParameters = new SimpleParameterSet(new Parameter[] {
				massDetectorTypeNumber, chromatogramBuilderTypeNumber,
				peakBuilderTypeNumber, suffix });

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

		for (int i = 0; i < chromatogramBuilderClasses.length; i++) {
			String className = chromatogramBuilderClasses[i] + "Parameters";
			Class paramClass;
			try {
				paramClass = Class.forName(className);
				chromatogramBuilderParameters[i] = (SimpleParameterSet) paramClass
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
	public SimpleParameterSet getChromatogramBuilderParameters(int ind) {
		return chromatogramBuilderParameters[ind];
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
		threeStepsParameters.setParameterValue(suffix, title);
	}

	/**
	 * 
	 * @return String
	 */
	public String getSuffix() {
		String Suffix = (String) threeStepsParameters.getParameterValue(suffix);
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
	 *            chromatogramBuilderInd
	 * @param int
	 *            peakBuilderInd
	 */
	public void setTypeNumber(int massDetectorInd, int chromatogramBuilderInd,
			int peakBuilderInd) {
		threeStepsParameters.setParameterValue(massDetectorTypeNumber,
				massDetectorInd);
		threeStepsParameters.setParameterValue(chromatogramBuilderTypeNumber,
				chromatogramBuilderInd);
		threeStepsParameters.setParameterValue(peakBuilderTypeNumber,
				peakBuilderInd);
	}

	/**
	 * 
	 * @return Integer massDetectorTypeNumber
	 */
	public int getMassDetectorTypeNumber() {
		return (Integer) threeStepsParameters
				.getParameterValue(massDetectorTypeNumber);
	}

	/**
	 * 
	 * @return Integer chromatogramBuilderTypeNumber
	 */
	public int getChromatogramBuilderTypeNumber() {
		return (Integer) threeStepsParameters
				.getParameterValue(chromatogramBuilderTypeNumber);
	}

	/**
	 * 
	 * @return Integer peakBuilderTypeNumber
	 */
	public int getPeakBuilderTypeNumber() {
		return (Integer) threeStepsParameters
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

		for (int i = 0; i < chromatogramBuilderParameters.length; i++) {
			Element subElement = element.addElement("chromatogrambuilder");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
					chromatogramBuilderNames[i]);
			chromatogramBuilderParameters[i].exportValuesToXML(subElement);
		}

		for (int i = 0; i < peakBuilderParameters.length; i++) {
			Element subElement = element.addElement("peakbuilder");
			subElement.addAttribute(PARAMETER_NAME_ATTRIBUTE,
					peakBuilderNames[i]);
			peakBuilderParameters[i].exportValuesToXML(subElement);
		}

		threeStepsParameters.exportValuesToXML(element);
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
					chromatogramBuilderParameters[i]
							.importValuesFromXML(paramElem);
					break;
				}
			}
		}

		Iterator paramIter3 = element.elementIterator("peakbuilder");
		while (paramIter3.hasNext()) {
			Element paramElem = (Element) paramIter3.next();
			for (int i = 0; i < massDetectorNames.length; i++) {
				if (paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE).equals(
						peakBuilderNames[i])) {
					peakBuilderParameters[i].importValuesFromXML(paramElem);
					break;
				}
			}
		}

		threeStepsParameters.importValuesFromXML(element);

	}

	/**
	 * This function allows to use these parameters by others threads. So it is
	 * possible to configure any other task with different parameters value
	 * without modify the behavior of other launched tasks
	 * 
	 */
	public ThreeStepPickerParameters clone() {

		ThreeStepPickerParameters newSet = new ThreeStepPickerParameters();

		newSet.massDetectorParameters = new SimpleParameterSet[massDetectorParameters.length];
		for (int i = 0; i < massDetectorParameters.length; i++) {
			newSet.massDetectorParameters[i] = massDetectorParameters[i]
					.clone();
		}

		newSet.chromatogramBuilderParameters = new SimpleParameterSet[chromatogramBuilderParameters.length];
		for (int i = 0; i < chromatogramBuilderParameters.length; i++) {
			newSet.chromatogramBuilderParameters[i] = chromatogramBuilderParameters[i]
					.clone();
		}

		newSet.peakBuilderParameters = new SimpleParameterSet[peakBuilderParameters.length];
		for (int i = 0; i < peakBuilderParameters.length; i++) {
			newSet.peakBuilderParameters[i] = peakBuilderParameters[i].clone();
		}

		newSet.threeStepsParameters = threeStepsParameters.clone();
		return newSet;

	}

}
