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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanAlignment;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record MassDetectionErrorStatistics(List<MassDetectionError> matchedSignals,
                                           int uniquePeaksInVendor, int uniquePeaksInOur,
                                           int totalInVendor) {

  public static MassDetectionErrorStatistics of(MassSpectrum vendor, @NotNull MassSpectrum detected,
      @NotNull MZTolerance tolerance, double minIntensity) {

    final SimpleSpectralArrays vendorDp = new SimpleSpectralArrays(vendor).filterGreaterNoise(
        minIntensity);
    final SimpleSpectralArrays detectedDp = new SimpleSpectralArrays(detected).filterGreaterNoise(
        minIntensity);
    final List<DataPoint[]> aligned = ScanAlignment.align(tolerance,
        DataPointUtils.getDataPoints(vendorDp.mzs(), vendorDp.intensities()),
        DataPointUtils.getDataPoints(detectedDp.mzs(), detectedDp.intensities()));

    List<MassDetectionError> matchedSignals = new ArrayList<>();
    int vendorOnly = 0;
    int oursOnly = 0;
    for (DataPoint[] dataPoints : aligned) {
      final var vendorCentroid = dataPoints[0];
      final var ourCentroid = dataPoints[1];

      matchedSignals.add(
          new MassDetectionError(vendorCentroid != null ? vendorCentroid.getMZ() : null,
              ourCentroid != null ? ourCentroid.getMZ() : null,
              vendorCentroid != null ? vendorCentroid.getIntensity() : null,
              ourCentroid != null ? ourCentroid.getIntensity() : null));

      if (vendorCentroid == null && ourCentroid != null) {
        oursOnly++;
      } else if (vendorCentroid != null && ourCentroid == null) {
        vendorOnly++;
      }
    }

    return new MassDetectionErrorStatistics(matchedSignals, vendorOnly, oursOnly,
        vendor.getNumberOfDataPoints());
  }
}
