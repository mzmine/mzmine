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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.NumberFormat;

/**
 * Parameters used by CentWaveDetector.
 */
public class CentWaveResolverParameters extends GeneralResolverParameters {

  /**
   * Peak integration methods.
   */
  public enum PeakIntegrationMethod {

    UseSmoothedData("Use smoothed data", 1), UseRawData("Use raw data", 2);

    private final String name;
    private final int index;

    /**
     * Create the method.
     *
     * @param aName   name
     * @param anIndex index (as used by findPeaks.centWave)
     */
    PeakIntegrationMethod(final String aName, final int anIndex) {

      name = aName;
      index = anIndex;
    }

    @Override
    public String toString() {

      return name;
    }

    public int getIndex() {

      return index;
    }
  }

  public static final DoubleRangeParameter PEAK_DURATION =
      new DoubleRangeParameter("Peak duration range", "Range of acceptable peak lengths",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public static final DoubleRangeParameter PEAK_SCALES = new DoubleRangeParameter("Wavelet scales",
      "Range wavelet widths (smallest, largest) in minutes",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));

  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("S/N threshold",
      "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 10.0, 0.0, null);

  public static final ComboParameter<PeakIntegrationMethod> INTEGRATION_METHOD =
      new ComboParameter<PeakIntegrationMethod>("Peak integration method",
          "Method used to determine RT extents of detected peaks", PeakIntegrationMethod.values(),
          PeakIntegrationMethod.UseSmoothedData);

  public CentWaveResolverParameters() {

    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, SN_THRESHOLD,
        PEAK_SCALES, PEAK_DURATION, INTEGRATION_METHOD, RENGINE_TYPE, MIN_NUMBER_OF_DATAPOINTS});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    final FeatureResolverSetupDialog dialog =
        new FeatureResolverSetupDialog(valueCheckRequired, this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public FeatureResolver getResolver() {
    return new CentWaveResolver();
  }
}
