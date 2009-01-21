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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class ExactMassDetectorParameters extends SimpleParameterSet {

	public static final String peakModelNames[] = { "Gaussian", "Gaussian plus triangle",
		"Lorentzian", "Lorentzian extended" };

	public static final String peakModelClasses[] = {
		"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.peakmodels.GaussPeak",
		"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.peakmodels.GaussPlusTrianglePeak",
		"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.peakmodels.LorentzianPeak",
		"net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass.peakmodels.LorentzianPeakWithShoulder" };

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter noiseLevel = new SimpleParameter(
			ParameterType.DOUBLE, "Noise level",
			"Intensities less than this value are interpreted as noise.",
			"absolute", new Double(10.0), new Double(0), null, MZmineCore
					.getIntensityFormat());

	public static final Parameter resolution = new SimpleParameter(
			ParameterType.INTEGER,
			"Mass resolution",
			"Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
					+ " Peak width is taken as the full width at half maximum intensity, (fwhm).",
			null, new Integer(60000), new Integer(0), null, NumberFormat
					.getIntegerInstance());

	public static final Parameter peakModel = new SimpleParameter(
			ParameterType.STRING,
			"Peak model function",
			"Lateral peaks under the curve of this peak model are not considered as a possible peak",
			null, peakModelNames);


	public ExactMassDetectorParameters() {
		super(new Parameter[] { noiseLevel, resolution, peakModel	});

	}

}
