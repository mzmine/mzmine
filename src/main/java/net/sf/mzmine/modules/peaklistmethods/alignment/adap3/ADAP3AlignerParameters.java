/* Copyright 2006-2019 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import dulab.adap.workflow.AlignmentParameters;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerParameters extends SimpleParameterSet {

    private static final String[] EIC_SCORE_TYPES = new String[]{
            AlignmentParameters.RT_DIFFERENCE,
            AlignmentParameters.CROSS_CORRELATION};

    public static final PeakListsParameter PEAK_LISTS =
            new PeakListsParameter();

    public static final StringParameter NEW_PEAK_LIST_NAME =
            new StringParameter("Aligned Peak List Name", "Peak list name", "Aligned peak list");

    public static final DoubleParameter SAMPLE_COUNT_RATIO =
            new DoubleParameter("Min confidence (between 0 and 1)",
                    "A fraction of the total number of samples. An aligned feature must be detected at " +
                            "least in several samples. This parameter determines the minimum number of samples where a " +
                            "feature must be detected.",
                    NumberFormat.getInstance(), 0.7, 0.0, 1.0);

    public static final RTToleranceParameter RET_TIME_RANGE = new RTToleranceParameter();

    public static final MZToleranceParameter MZ_RANGE = new MZToleranceParameter();

    public static final DoubleParameter SCORE_TOLERANCE =
            new DoubleParameter("Score threshold (between 0 and 1)",
                    "The minimum value of the similarity function required for features to be aligned together.",
                    NumberFormat.getInstance(), 0.75, 0.0, 1.0);

    public static final DoubleParameter SCORE_WEIGHT =
            new DoubleParameter("Score weight (between 0 and 1)",
                    "The weight w that is used in the similarity function. See the help file for details.",
                    NumberFormat.getInstance(), 0.1, 0.0, 1.0);

    public static final ComboParameter<String> EIC_SCORE =
            new ComboParameter<>("Retention time similarity",
                    "Method used for calculating the retention time similarity. The retention time difference " +
                            "(fast) is preferred method.",
                    EIC_SCORE_TYPES,
                    AlignmentParameters.RT_DIFFERENCE);

    public ADAP3AlignerParameters() {
        super(new Parameter[]{PEAK_LISTS, SAMPLE_COUNT_RATIO, RET_TIME_RANGE, MZ_RANGE,
                SCORE_TOLERANCE, SCORE_WEIGHT, EIC_SCORE, NEW_PEAK_LIST_NAME});
    }
}
