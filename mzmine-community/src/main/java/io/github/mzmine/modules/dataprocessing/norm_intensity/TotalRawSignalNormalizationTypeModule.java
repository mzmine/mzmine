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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Normalize by average TIC across mass lists in selected scans (used to build chromatograms).
 * Average to factor out the number of scans and scan rate. Use mass lists because they remove
 * already parts of the noise that may skew the normalization.
 */
public class TotalRawSignalNormalizationTypeModule extends AbstractFactorNormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return NormalizationType.TotalRawSignal.toString();
  }

  @Override
  public final @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return TotalRawSignalNormalizationTypeParameters.class;
  }

  @Override
  protected double getNormalizationMetricForFile(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull final RawDataFile file,
      @NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet linearNormalizerParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {

    // The total raw signal is a per-file normalization metric and is applied as a single factor per
    // RawDataFile, so there is no per-row scan selection context here. For polarity switching data
    // this therefore uses the merged scans across all scan selections of the file (combined signal).
    // decision: per-file metric cannot attribute to a single selection - see getAllScansForFile.
    if (!featureList.getSelectedScansData().hasScansForFile(file)) {
      throw new IllegalStateException("No scans selected for datafile: " + file.getName());
    }
    // merged scans may still be empty (a selection was registered with an empty scan list) - that is
    // handled by the "No TIC found" check below to preserve the previous behavior
    final List<? extends Scan> scans = featureList.getSelectedScansData().getAllScansForFile(file);
    // calculate average TIC of mass lists instead of raw scans - raw scans may be dominated by noise
    // average TIC to factor out the scan rate
    ScanUtils.assertMassLists(scans);

    final double avgTIC = scans.stream().map(Scan::getMassList)
        .mapToDouble(scan -> Objects.requireNonNullElse(scan.getTIC(), 0d)).average().orElse(0d);
    if (Double.compare(avgTIC, 0d) == 0) {
      throw new IllegalStateException("No TIC found for file: " + file.getName());
    }
    return avgTIC;
  }
}
