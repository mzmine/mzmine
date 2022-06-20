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

package io.github.mzmine.modules.tools.batchwizard.defaults;

import com.google.common.collect.Range;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardHPLCParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;

public class DefaultLcParameters {

  public static final DefaultLcParameters uhplc = new DefaultLcParameters(Range.closed(0.3, 30d),
      new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final DefaultLcParameters hplc = new DefaultLcParameters(Range.closed(0.5, 60d),
      new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.08f, Unit.MINUTES),
      new RTTolerance(0.4f, Unit.MINUTES));

  public static final DefaultLcParameters gc = new DefaultLcParameters(true, Range.closed(0.5, 60d),
      3, 1, 50, new RTTolerance(0.03f, Unit.MINUTES), new RTTolerance(0.03f, Unit.MINUTES),
      new RTTolerance(0.08f, Unit.MINUTES));

  private final boolean stableIonizationAcrossSamples;
  private final Range<Double> cropRtRange;
  private final int minNumberDataPoints;
  private final int minSamples;
  private final int maxIsomersInChromatogram;
  private final RTTolerance fwhm;
  private final RTTolerance intraSampleTolerance;
  private final RTTolerance interSampleTolerance;

  public DefaultLcParameters(Range<Double> rtRange, RTTolerance fwhm,
      RTTolerance intraSampleTolerance, RTTolerance interSampleTolerance) {
    this(true, rtRange, 4, 1, 15, fwhm, intraSampleTolerance, interSampleTolerance);
  }

  public DefaultLcParameters(boolean stableIonizationAcrossSamples, Range<Double> cropRtRange,
      int minNumberDataPoints, int minSamples, int maxIsomersInChromatogram, RTTolerance fwhm,
      RTTolerance intraSampleTolerance, RTTolerance interSampleTolerance) {
    this.stableIonizationAcrossSamples = stableIonizationAcrossSamples;
    this.cropRtRange = cropRtRange;
    this.minNumberDataPoints = minNumberDataPoints;
    this.minSamples = minSamples;
    this.maxIsomersInChromatogram = maxIsomersInChromatogram;
    this.fwhm = fwhm;
    this.intraSampleTolerance = intraSampleTolerance;
    this.interSampleTolerance = interSampleTolerance;
  }

  public void setToParameterSet(ParameterSet param) {
    param.setParameter(BatchWizardHPLCParameters.stableIonizationAcrossSamples,
        stableIonizationAcrossSamples);
    param.setParameter(BatchWizardHPLCParameters.minNumberOfSamples, minSamples);
    param.setParameter(BatchWizardHPLCParameters.cropRtRange, cropRtRange);
    param.setParameter(BatchWizardHPLCParameters.maximumIsomersInChromatogram,
        maxIsomersInChromatogram);
    param.setParameter(BatchWizardHPLCParameters.minNumberOfDataPoints, minNumberDataPoints);
    param.setParameter(BatchWizardHPLCParameters.approximateChromatographicFWHM, fwhm);
    param.setParameter(BatchWizardHPLCParameters.intraSampleRTTolerance, intraSampleTolerance);
    param.setParameter(BatchWizardHPLCParameters.interSampleRTTolerance, interSampleTolerance);
  }
}
