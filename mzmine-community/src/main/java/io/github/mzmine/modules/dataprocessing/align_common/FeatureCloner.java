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

package io.github.mzmine.modules.dataprocessing.align_common;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.modules.dataprocessing.align_gc.GCConsensusAlignerPostProcessor;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;

public sealed interface FeatureCloner {

  /**
   * @param feature           to clone
   * @param targetFeatureList the target feature list that the feature will be added to (not yet
   *                          done in this method)
   * @param targetAlignRow    the target row that the feature will be added (not yet done in this
   *                          method)
   * @return new feature either directly cloned or newly extracted
   */
  @NotNull ModularFeature cloneFeature(final Feature feature,
      final ModularFeatureList targetFeatureList, final FeatureListRow targetAlignRow);

  /**
   * Always just clones the feature.
   */
  record SimpleFeatureCloner() implements FeatureCloner {

    @Override
    public @NotNull ModularFeature cloneFeature(final Feature feature,
        final ModularFeatureList targetFeatureList, final FeatureListRow targetAlignRow) {
      return new ModularFeature(targetFeatureList, feature);
    }

  }

  /**
   * Currently unused - previously this was used for GC but now GC aligner just clones the orginal
   * features and later the {@link GCConsensusAlignerPostProcessor} will find a consensus main
   * feature.
   * <p>
   * On mz mismatch between row and feature, this cloner extracts a new series to create a
   * completely new feature. This is useful for GC alignment as features in GC are based on random
   * representative m/z for a pseudo spectrum feature
   */
  record ExtractMzMismatchFeatureCloner(MZTolerance mzTolerance) implements FeatureCloner {

    @Override
    public @NotNull ModularFeature cloneFeature(final Feature privFeature,
        final ModularFeatureList targetFeatureList, final FeatureListRow targetAlignRow) {
      ModularFeature feature = (ModularFeature) privFeature;

      Range<Double> mzTolRange = mzTolerance.getToleranceRange(targetAlignRow.getAverageMZ());
      if (mzTolRange.contains(feature.getMZ())) {
        return new ModularFeature(targetFeatureList, feature);
      } else {
        // mz mismatch, because GC retains a random m/z as a representative for a feature (deconvoluted pseudo spectrum)
        RawDataFile dataFile = feature.getRawDataFile();
        IonTimeSeries<Scan> ionTimeSeries = IonTimeSeriesUtils.extractIonTimeSeries(dataFile,
            feature.getScanNumbers(), mzTolRange, feature.getRawDataPointsRTRange(),
            dataFile.getMemoryMapStorage());

        final ModularFeature newFeature;
        newFeature = new ModularFeature(targetFeatureList, feature);
        newFeature.set(FeatureDataType.class, ionTimeSeries);
        FeatureDataUtils.recalculateIonSeriesDependingTypes(newFeature);
        return newFeature;
      }
    }
  }

}
