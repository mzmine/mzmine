/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.Objects;

public class FeatureStatistics {

  final List<FeatureWithIsotopeTraces> envelopes;
  final FeatureWithIsotopeTraces bestEnvelope;

  public FeatureStatistics(List<FeatureWithIsotopeTraces> envelopes) {
    this.envelopes = envelopes;
    bestEnvelope = FeatureWithIsotopeTraces.getBest(envelopes);
  }

  /**
   * @param isoSeries   the series to extract values from
   * @param intensities a list to put the intensity values into
   */
  private static void extractFirstAndLastNonZeroIntensity(IonTimeSeries<? extends Scan> isoSeries,
      DoubleArrayList intensities) {

    // since we use the full series, we expect some zeros to be there. if there are no zeros,
    // we are also not close to the noise level, so disregard those isotope traces

    boolean previousPointZero = false;
    int stoppedAtIndex = 0;
    for (int i = 0; i < isoSeries.getNumberOfValues(); i++) {
      if (previousPointZero && isoSeries.getIntensity(i) > 0) {
        intensities.add(isoSeries.getIntensity(i));
        stoppedAtIndex = i;
        break;
      }
      if (Double.compare(isoSeries.getIntensity(i), 0d) <= 0) {
        previousPointZero = true;
      }
    }

    // only add the same point once, so we stop as soon as we reach the same index as we had before
    previousPointZero = false;
    for (int i = isoSeries.getNumberOfValues() - 1; i > 0 && i > stoppedAtIndex; i--) {
      if (previousPointZero && isoSeries.getIntensity(i) > 0) {
        intensities.add(isoSeries.getIntensity(i));
        break;
      }
      if (Double.compare(isoSeries.getIntensity(i), 0d) <= 0) {
        previousPointZero = true;
      }
    }
  }

  public MZTolerance getBestTolerance() {
    return bestEnvelope.mzTolerance();
  }

  public double getMz() {
    return bestEnvelope.mainFeature().getMZ();
  }

  public double[] getBestIsotopesFWHMs() {
    return bestEnvelope.isotopeTraces().stream().map(Feature::getFWHM).filter(Objects::nonNull)
        .mapToDouble(Float::doubleValue).toArray();
  }

  /**
   * @return get the first and last non-zero intensities of the isotope peaks. may be used to
   * estimate the noise level
   */
  public double[] getNonZeroIsotopeEdgeIntensities() {
    final DoubleArrayList intensities = new DoubleArrayList();

    for (ModularFeature iso : bestEnvelope.isotopeTraces()) {
      final IonTimeSeries<? extends Scan> isoSeries = iso.getFeatureData();
      extractFirstAndLastNonZeroIntensity(isoSeries, intensities);
    }

    return intensities.toDoubleArray();
  }

  public List<FeatureWithIsotopeTraces> getEnvelopes() {
    return envelopes;
  }

  public FeatureWithIsotopeTraces getBestEnvelope() {
    return bestEnvelope;
  }

  @Override
  public String toString() {
    return "m/z %.4f, RT %.2f min".formatted(getMz(), getBestEnvelope().mainFeature().getRT());
  }
}
