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

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

/**
 * Holds baseline correction module parameters.
 *
 * @author Chris Pudney, Syngenta Ltd
 * @version $Revision$
 */
public class BaselineCorrectionParameters extends SimpleParameterSet {

    /**
     * TIC chromatogram label.
     */
    public static final String CHROMATOGRAM_TIC = "TIC";

    /**
     * Base peak intensity chromatogram label.
     */
    public static final String CHROMATOGRAM_BP = "Base peak intensity";

    /**
     * Raw data file suffix.
     */
    public static final Parameter SUFFIX = new SimpleParameter(
            ParameterType.STRING,
            "Filename suffix",
            "Suffix to be appended to raw data file names.",
            null,
            "baseline-corrected",
            null);

    /**
     * Smoothing factor.
     */
    public static final Parameter CHROMOTAGRAM_TYPE = new SimpleParameter(
            ParameterType.STRING,
            "Chromatogram type",
            "The type of chromatogram from which infer a baseline to be corrected.",
            CHROMATOGRAM_TIC,
            new String[]{CHROMATOGRAM_TIC, CHROMATOGRAM_BP});

    /**
     * Smoothing factor.
     */
    public static final Parameter SMOOTHING = new SimpleParameter(
            ParameterType.DOUBLE,
            "Smoothing",
            "The smoothing factor, generally 10^5 - 10^8, the larger it is, the smoother the baseline will be.",
            null,
            1.0e7,
            0.0,
            null);

    /**
     * Asymmetry.
     */
    public static final Parameter ASYMMETRY = new SimpleParameter(
            ParameterType.DOUBLE,
            "Asymmetry",
            "The weight (p) for points above the trendline, whereas 1-p is the weight for points below it.  Naturally, p should be small for estimating baselines.",
            null,
            0.001,
            0.0,
            1.0);

    /**
     * Apply in bins.
     */
    public static final Parameter USE_MZ_BINS = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Use m/z bins",
            "If checked, then full m/z range will be divided into bins a baseline correction applied to each bin (see m/z bin width).",
            true);

    /**
     * M/Z bin width.
     */
    public static final Parameter MZ_BIN_WIDTH = new SimpleParameter(
            ParameterType.DOUBLE,
            "m/z bin width",
            "The m/z bin size to use when the \"" + USE_MZ_BINS.getName() + "\" option is enabled.",
            null,
            1.0,
            0.001,
            null,
            MZmineCore.getMZFormat());

    /**
     * MS-level.
     */
    public static final Parameter MS_LEVEL = new SimpleParameter(
            ParameterType.INTEGER,
            "MS-level",
            "The MS-level at which to apply the baseline correction (choose 0 for all levels).",
            1,
            new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

    /**
     * Remove original data file.
     */
    public static final Parameter REMOVE_ORIGINAL = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove source file after baseline correction",
            "If checked, original file will be replaced by the corrected version",
            true);

    /**
     * Create the parameter set.
     */
    public BaselineCorrectionParameters() {
        super(new Parameter[]{SUFFIX, CHROMOTAGRAM_TYPE, MS_LEVEL, SMOOTHING, ASYMMETRY, USE_MZ_BINS, MZ_BIN_WIDTH, REMOVE_ORIGINAL});
    }
}

