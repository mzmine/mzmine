/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.tof;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.collections.SimpleIndexRange;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TofMassDetector implements MassDetector {

  private static final Logger logger = Logger.getLogger(TofMassDetector.class.getName());

  private final double noiseLevel;

  public TofMassDetector() {
    this(0);
  }

  public TofMassDetector(double noiseLevel) {
    this.noiseLevel = noiseLevel;
  }

  @Override
  public MassDetector create(ParameterSet params) {
    return new TofMassDetector(params.getValue(TofMassDetectorParameters.noiseLevel));
  }

  @Override
  public boolean filtersActive() {
    return false;
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum) {
    if (spectrum.getNumberOfDataPoints() < 3) {
      return new double[2][0];
    }

    final double maxDiff = getMaxMzDiff(spectrum);
    logger.fine("Max mz diff: " + maxDiff);

    final List<IndexRange> consecutiveRanges = new ArrayList<>();
    int currentRegionStart = 0;
    boolean inRegion = false;
    double lastMz = spectrum.getMzValue(0);
    for (int i = 1; i < spectrum.getNumberOfDataPoints(); i++) {
      final double thisMz = spectrum.getMzValue(i);
      final double mzDelta = thisMz - lastMz;
      if (mzDelta >= maxDiff) {
        if (i - currentRegionStart > 3) {
          consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, i));
        }
        inRegion = false;
        currentRegionStart = i;
      } else if (mzDelta < maxDiff && !inRegion) {
        inRegion = true;
        currentRegionStart = i;
      }
      lastMz = thisMz;
    }


    return new double[2][0];
  }

  private static double getMaxMzDiff(MassSpectrum spectrum) {
    for (int i = spectrum.getNumberOfDataPoints() - 1; i > 1; i--) {
      // tof mz value distances are proportional to sqrt(m/z)
      // so the biggest mass diff will be at the top of the spectrum
      if (Double.compare(spectrum.getIntensityValue(i), 0) != 0 || !(
          spectrum.getIntensityValue(i - 1) > 0)) {
        continue;
      }
      return Math.abs(spectrum.getMzValue(i) - spectrum.getMzValue(i - 1));
    }
    return 0;
  }

  @Override
  public @NotNull String getName() {
    return "TOF mass detector";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TofMassDetectorParameters.class;
  }
}
