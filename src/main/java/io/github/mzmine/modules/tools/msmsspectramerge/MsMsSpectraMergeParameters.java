/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.tools.msmsspectramerge;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MsMsSpectraMergeParameters extends SimpleParameterSet {

  public MsMsSpectraMergeParameters() {
    super(new Parameter[]{MERGE_MODE, MZ_MERGE_MODE, INTENSITY_MERGE_MODE, MASS_ACCURACY,
            COSINE_PARAMETER, REL_SIGNAL_COUNT_PARAMETER, ISOLATION_WINDOW_OFFSET,
            ISOLATION_WINDOW_WIDTH},
        "https://mzmine.github.io/mzmine_documentation/module_docs/GNPS_export/merge_ms2_kai.html");
  }

  public static final PercentParameter COSINE_PARAMETER = new PercentParameter(
      "Cosine threshold (%)",
      "Threshold for the cosine similarity between two spectra for merging. Set to 0 if the spectra may have different collision energy!",
      0.7d, 0d, 1d);

  public static final PercentParameter REL_SIGNAL_COUNT_PARAMETER = new PercentParameter(
      "Signal count threshold (%)",
      "After merging, remove all signals which occur in less than X % of the merged spectra.", 0.2d,
      0d, 1d);

  public static final MZToleranceParameter MASS_ACCURACY = new MZToleranceParameter(
      "Expected mass deviation",
      "Expected mass deviation of your measurement in ppm (parts per million) or Da (larger value is used). We recommend to use a rather large value, e.g. 10 ppm for Orbitrap, 15 ppm for Q-ToF, 100 ppm for QQQ.");

  public static final ComboParameter<MzMergeMode> MZ_MERGE_MODE = new ComboParameter<MzMergeMode>(
      "m/z merge mode",
      "How to merge the m/z values of features from different spectra with similar mass. Choose 'most intense' to pick always the m/z of the best feature - this is a very conservative and safe option. However, 'weighted average (cuttoff outliers)' will often have better results.",
      MzMergeMode.values(), MzMergeMode.WEIGHTED_AVERAGE_CUTOFF_OUTLIERS);

  public static final ComboParameter<IntensityMergeMode> INTENSITY_MERGE_MODE = new ComboParameter<IntensityMergeMode>(
      "intensity merge mode",
      "How to merge the intensity values of features from different spectra with similar mass. 'sum intensities' is a convenient option that will increase the intensities of features that occur consistently in many fragment scans. However, this will make intensities between merged and unmerged spectra incomparable. Use 'max intensitiy' if you want to preserve intensity values.",
      IntensityMergeMode.values(), IntensityMergeMode.SUM);

  public static final ComboParameter<MergeMode> MERGE_MODE = new ComboParameter<MergeMode>(
      "Select spectra to merge",
      "'across samples' is a convenient option that will merge all MS/MS which belong to the same feature. Note that a clustering is performed automatically to filter out MS/MS which are wrongly associated with the same feature. However, 'same sample' might sometimes be the safer option if you do not thrust your alignment algorithm.",
      MergeMode.values(), MergeMode.ACROSS_SAMPLES);

  public static final DoubleParameter ISOLATION_WINDOW_OFFSET = new DoubleParameter(
      "Isolation window offset (m/z)", "isolation window offset from the precursor m/z",
      MZmineCore.getConfiguration().getMZFormat(), 0d);

  public static final DoubleParameter ISOLATION_WINDOW_WIDTH = new DoubleParameter(
      "Isolation window width (m/z)", "width (left and right from offset) of the isolation window",
      MZmineCore.getConfiguration().getMZFormat(), 3d);

}
