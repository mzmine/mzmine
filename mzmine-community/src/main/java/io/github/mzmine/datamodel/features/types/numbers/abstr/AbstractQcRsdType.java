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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.modifiers.MinSamplesRequirement;
import io.github.mzmine.datamodel.features.types.modifiers.NoDataColumnType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.util.MathUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates the relative standard deviation (RSD, coefficient of variation) of a numeric feature
 * type, e.g. {@link AreaType}, over all quality control samples of the feature list.
 * <p>
 * The value is computed on demand and never stored, therefore this type is a {@link MappingType}
 * and a {@link NoDataColumnType}. This also means that the RSD always uses the current sample type
 * metadata.
 */
public abstract class AbstractQcRsdType extends PercentType implements MappingType<Float>,
    MinSamplesRequirement, NoDataColumnType {

  private static final SampleTypeFilter QC_FILTER = SampleTypeFilter.qc();

  /**
   * @return the feature type this RSD is calculated for, e.g. {@link AreaType}
   */
  protected abstract @NotNull DataType<? extends Number> getFeatureType();

  @Override
  public @Nullable Float getValue(@NotNull final ModularDataModel model) {
    // only rows have features over multiple samples to calculate the RSD from
    return model instanceof FeatureListRow row ? calculateRsd(row) : null;
  }

  /**
   * @return the RSD over all QC samples or null if there are less than {@link #getMinSamples()} QC
   * samples or if the sum over all QC samples is 0.
   */
  private @Nullable Float calculateRsd(@NotNull final FeatureListRow row) {
    final List<RawDataFile> qcFiles = QC_FILTER.filterFiles(row.getFeatureList().getRawDataFiles());
    // sample stdev (n-1) needs at least two values
    if (qcFiles.size() < getMinSamples()) {
      return null;
    }

    final DataType<? extends Number> featureType = getFeatureType();
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
    return (float) MathUtils.calcRelativeStd(values);
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
