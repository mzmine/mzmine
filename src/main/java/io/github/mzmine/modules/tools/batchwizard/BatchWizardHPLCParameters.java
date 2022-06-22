/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class BatchWizardHPLCParameters extends SimpleParameterSet {

  public static final RTToleranceParameter approximateChromatographicFWHM = new RTToleranceParameter(
      "Approximate feature FWHM",
      "The approximate feature width (chromatograpic peak width) in retention time (full-width-at-half-maximum, FWHM). ");

  public static final RTToleranceParameter intraSampleRTTolerance = new RTToleranceParameter(
      "Intra-sample RT tolerance",
      "Retention time tolerance for multiple signals of the same compound in the same "
          + "sample.\nUsed to detect isotopes or multimers/adducts of the same compound.");

  public static final RTToleranceParameter interSampleRTTolerance = new RTToleranceParameter(
      "Inter-sample RT tolerance",
      "Retention time tolerance for the same compound in different samples.\n"
          + "Used to align multiple measurements of the same sample or a batch run.");

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Min # of data points",
      "Minimum number of data points as used in chromatogram building and feature resolving.", 4, 1,
      Integer.MAX_VALUE);

  public static final IntegerParameter minNumberOfSamples = new IntegerParameter(
      "Min samples per aligned feature",
      "The minimum number of samples in which a feature needs to be detected, e.g., 2-3 for triplicates.\n"
          + "Used in feature list rows filter and feature grouping.", 1, 1, Integer.MAX_VALUE);

  public static final RTRangeParameter cropRtRange = new RTRangeParameter("Crop retention time",
      "Crops the RT range of chromatograms. Used to exclude time before the flow time\n"
          + "and after the separation, where in many runs cleaning and re-equilibration starts.",
      true, Range.closed(0.5, 30d));

  public static final IntegerParameter maximumIsomersInChromatogram = new IntegerParameter(
      "Max peaks in chromatogram",
      "An estimate maximum number of peaks in a chromatogram (number of same m/z features).\n"
          + "Used to estimate the chromatographic threshold to filter noisy chromatograms.", 10, 1,
      Integer.MAX_VALUE);

  public static final BooleanParameter stableIonizationAcrossSamples = new BooleanParameter(
      "Stable ionization across samples",
      "Only check if the ionization conditions are stable across all samples.\n"
          + "Uncheck for varying salt content or variations in ionization conditions.\n"
          + "Used in feature grouping.", true);


  public static final BooleanParameter filter13C = new BooleanParameter(
      "Only keep features with 13C",
      "Filters out all rows that have no feature with a 13C isotope pattern", true);

  public static final OriginalFeatureListHandlingParameter handleOriginalFeatureLists = new OriginalFeatureListHandlingParameter(
      false, OriginalFeatureListOption.REMOVE);

  public BatchWizardHPLCParameters() {
    super(new Parameter[]{stableIonizationAcrossSamples, cropRtRange, maximumIsomersInChromatogram,
        minNumberOfSamples, minNumberOfDataPoints, approximateChromatographicFWHM,
        intraSampleRTTolerance, interSampleRTTolerance, filter13C, handleOriginalFeatureLists});
  }
}
