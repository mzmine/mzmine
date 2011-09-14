/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import java.text.DecimalFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * Holds baseline correction module parameters.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class BaselineCorrectionParameters extends SimpleParameterSet {

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	/**
	 * Raw data file suffix.
	 */
	public static final StringParameter SUFFIX = new StringParameter(
			"Filename suffix", "Suffix to be appended to raw data file names.",
			"baseline-corrected");

	/**
	 * Smoothing factor.
	 */
	public static final ComboParameter<ChromatogramType> CHROMOTAGRAM_TYPE = new ComboParameter<ChromatogramType>(
			"Chromatogram type",
			"The type of chromatogram from which infer a baseline to be corrected.",
			ChromatogramType.values(), ChromatogramType.TIC);

	/**
	 * Smoothing factor.
	 */
	public static final DoubleParameter SMOOTHING = new DoubleParameter(
			"Smoothing",
			"The smoothing factor (>= 0), generally 10^5 - 10^8, the larger it is, the smoother the baseline will be.",
			DecimalFormat.getNumberInstance(), null, 0.0, null);

	/**
	 * Asymmetry.
	 */
	public static final DoubleParameter ASYMMETRY = new DoubleParameter(
			"Asymmetry",
			"The weight (0 <= p <= 1) for points above the trend line, whereas 1-p is the weight for points below it.  Naturally, p should be small for estimating baselines.",
			DecimalFormat.getNumberInstance(), 0.001, 0.0, 1.0);

	/**
	 * Apply in bins.
	 */
	public static final BooleanParameter USE_MZ_BINS = new BooleanParameter(
			"Use m/z bins",
			"If checked, then full m/z range will be divided into bins a baseline correction applied to each bin (see m/z bin width).",
			true);

	/**
	 * M/Z bin width.
	 */
	public static final DoubleParameter MZ_BIN_WIDTH = new DoubleParameter(
			"m/z bin width", "The m/z bin size (>= 0.001) to use when the \""
					+ USE_MZ_BINS.getName() + "\" option is enabled.",
			MZmineCore.getMZFormat(), 1.0, 0.001, null);

	/**
	 * MS-level.
	 */
	public static final MSLevelParameter MS_LEVEL = new MSLevelParameter();

	/**
	 * Remove original data file.
	 */
	public static final BooleanParameter REMOVE_ORIGINAL = new BooleanParameter(
			"Remove source file after baseline correction",
			"If checked, original file will be replaced by the corrected version",
			true);

	/**
	 * Create the parameter set.
	 */
	public BaselineCorrectionParameters() {
		super(new Parameter[] { dataFiles, SUFFIX, CHROMOTAGRAM_TYPE, MS_LEVEL,
				SMOOTHING, ASYMMETRY, USE_MZ_BINS, MZ_BIN_WIDTH,
				REMOVE_ORIGINAL });
	}
}
