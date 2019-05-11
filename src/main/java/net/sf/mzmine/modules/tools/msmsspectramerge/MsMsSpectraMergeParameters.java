/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

import java.text.NumberFormat;
import java.util.Locale;

public class MsMsSpectraMergeParameters extends SimpleParameterSet {

    public MsMsSpectraMergeParameters() {
        super(
                new Parameter[]{MERGE_MODE, MZ_MERGE_MODE, INTENSITY_MERGE_MODE, MASS_ACCURACY, COSINE_PARAMETER, PEAK_COUNT_PARAMETER, ISOLATION_WINDOW_OFFSET, ISOLATION_WINDOW_WIDTH}
        );
    }

    public static final PercentParameter COSINE_PARAMETER = new PercentParameter("Cosine threshold (%)", "Threshold for the cosine similarity between two spectra for merging. Set to 0 if the spectra may have different collision energy!", 0.7d, 0d, 1d);

    public static final PercentParameter PEAK_COUNT_PARAMETER = new PercentParameter("Peak count threshold ", "After merging, remove all peaks which occur in less than X % of the merged spectra.", 0.2d, 0d, 1d);

    public static final MZToleranceParameter MASS_ACCURACY = new MZToleranceParameter("Expected mass deviation", "Expected mass deviation of your measurement in ppm (parts per million) and Dalton. We recommend to use a rather large value, e.g. 10 for Orbitrap, 15 for Q-ToF, 100 for QQQ.");

    public static final ComboParameter<MzMergeMode> MZ_MERGE_MODE = new ComboParameter<MzMergeMode>("m/z merge mode", "How to merge the m/z values of peaks from different spectra with similar mass. Choose 'most intense' to pick always the m/z of the best peak - this is a very conservative and safe option. However, 'weighted average (cuttoff outliers)' will often have better results.", MzMergeMode.values(), MzMergeMode.WEIGHTED_AVERAGE_CUTOFF_OUTLIERS);

    public static final ComboParameter<IntensityMergeMode> INTENSITY_MERGE_MODE = new ComboParameter<IntensityMergeMode>("intensity merge mode", "How to merge the intensity values of peaks from different spectra with similar mass. 'sum intensities' is a convenient option that will increase the intensities of peaks that occur consistently in many fragment scans. However, this will make intensities between merged and unmerged spectra incomparable. Use 'max intensitiy' if you want to preserve intensity values.", IntensityMergeMode.values(), IntensityMergeMode.SUM);

    public static final ComboParameter<MergeMode> MERGE_MODE = new ComboParameter<MergeMode>("Select spectra to merge", "'across samples' is a convenient option that will merge all MS/MS which belong to the same feature. Note that a clustering is performed automatically to filter out MS/MS which are wrongly associated with the same feature. However, 'same sample' might sometimes be the safer option if you do not thrust your alignment algorithm.",
                    MergeMode.values(), MergeMode.ACROSS_SAMPLES);

    public static final DoubleParameter ISOLATION_WINDOW_OFFSET = new DoubleParameter("Isolation window offset", "isolation window offset from the precursor m/z", NumberFormat.getNumberInstance(Locale.US), 0d);
    
    public static final DoubleParameter ISOLATION_WINDOW_WIDTH = new DoubleParameter("Isolation window width", "width (left and right from offset) of the isolation window", NumberFormat.getNumberInstance(Locale.US), 3d);


}
