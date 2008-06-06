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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class ExactMassDetectorParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter noiseLevel = new SimpleParameter(
			ParameterType.FLOAT, "Noise level",
			"Intensities less than this value are interpreted as noise",
			"absolute", new Float(10.0), new Float(0.0), null, MZmineCore
					.getIntensityFormat());

	public static final Parameter resolution = new SimpleParameter(
			ParameterType.INTEGER,
			"Mass Resolution",
			"Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
					+ " Peak width is taken as the full width at half maximum intensity, (fwhm).",
			null, new Integer(60000), new Integer(0), null, NumberFormat
					.getIntegerInstance());

	public static final Parameter cleanLateral = new SimpleParameter(
			ParameterType.BOOLEAN,
			"Remove FTMS shoulder peaks",
			"Remove lateral peaks under the criteria defined by percentage of peak's intensity and resolution",
			null, new Boolean(false), null, null, null);

	public static final Parameter percentageHeight = new SimpleParameter(
			ParameterType.FLOAT,
			"Percentage of Itensity",
			"Intensities less than this percentage of the biggest peak's instensity in a range (see % of Resolution) are removed",
			"%", new Float(0.05), new Float(0.0), null, percentFormat);

	public static final Parameter percentageResolution = new SimpleParameter(
			ParameterType.FLOAT,
			"Percentage of Resolution",
			"According with this percentage, it is estimated the base peak's width. This width is used to define "
					+ "the range for seeking lateral peaks. Peaks with mass within this width are removed ",
			"%", new Float(0.25), new Float(0.01), null, percentFormat);

	public ExactMassDetectorParameters() {
		super(new Parameter[] { noiseLevel, resolution, cleanLateral,
				percentageHeight, percentageResolution });

	}

}
