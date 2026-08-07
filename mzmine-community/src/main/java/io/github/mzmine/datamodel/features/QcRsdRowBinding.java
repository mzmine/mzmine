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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.MinSamplesRequirement;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.util.MathUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates the relative standard deviation (RSD, coefficient of variation) of a numeric feature
 * type, e.g. {@link AreaType}, over all quality control samples of the feature list.
 */
public class QcRsdRowBinding implements RowBinding {

  /**
   * Sample stdev (n-1) needs at least two values, matching {@link MinSamplesRequirement}.
   */
  private static final int MIN_QC_SAMPLES = 2;

  private static final SampleTypeFilter QC_FILTER = SampleTypeFilter.qc();

  private final @NotNull DataType<Float> rowType;
  private final @NotNull DataType<? extends Number> featureType;

  public QcRsdRowBinding(@NotNull final DataType<Float> rowType,
      @NotNull final DataType<? extends Number> featureType) {
    this.rowType = rowType;
    this.featureType = featureType;
  }

  @Override
  public void apply(@Nullable final FeatureListRow row) {
    // row might be null if the feature was not yet added
    if (row != null) {
      row.set(rowType, calculateRsd(row));
    }
  }

  /**
   * @return the RSD over all QC samples or null if there are less than {@link #MIN_QC_SAMPLES} QC
   * samples or if the sum over all QC samples is 0.
   */
  private @Nullable Float calculateRsd(@NotNull final FeatureListRow row) {
    // not cached so that the RSD always uses the current sample type metadata
    final List<RawDataFile> qcFiles = QC_FILTER.filterFiles(row.getFeatureList().getRawDataFiles());
    if (qcFiles.size() < MIN_QC_SAMPLES) {
      return null;
    }

    final double[] values = new double[qcFiles.size()];
    double sum = 0;
    for (int i = 0; i < qcFiles.size(); i++) {
      // decision: undetected features in a QC sample count as 0 abundance
      final Number value =
          row.getFeature(qcFiles.get(i)) instanceof ModularDataModel feature ? feature.get(
              featureType) : null;
      values[i] = value == null ? 0d : value.doubleValue();
      sum += values[i];
    }

    // no signal at all in the QC samples: RSD is undefined. NaN may occur for normalized values
    if (Double.compare(sum, 0d) == 0 || !Double.isFinite(sum)) {
      return null;
    }
    return (float) MathUtils.calcRelativeStd(values) * 100f;
  }

  @Override
  public DataType getRowType() {
    return rowType;
  }

  @Override
  public DataType getFeatureType() {
    return featureType;
  }

  @Override
  public void valueChanged(final ModularDataModel dataModel, final DataType type,
      final Object oldValue, final Object newValue) {
    if (dataModel instanceof Feature feature) {
      // change in feature applied to its row
      apply(feature.getRow());
    } else {
      throw new UnsupportedOperationException(
          "Cannot apply a QcRsdRowBinding if the changed data model is not a Feature");
    }
  }
}
