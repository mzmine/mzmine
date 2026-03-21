/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelection;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class NormIntensityTestUtils {

  private NormIntensityTestUtils() {
  }

  static @NotNull RawDataFileImpl createRawFile(final @NotNull String name,
      final @Nullable LocalDateTime timestamp) {
    final RawDataFileImpl rawDataFile = new RawDataFileImpl(name, null, null);
    rawDataFile.setStartTimeStamp(timestamp);
    return rawDataFile;
  }

  static @NotNull ParameterSet createMainParameters(
      final @NotNull AbundanceMeasure abundanceMeasure) {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm",
        NormalizationType.MedianFeatureIntensity, createFactorParameters(), abundanceMeasure,
        OriginalFeatureListOption.KEEP, List.of());
  }

  static @NotNull FactorNormalizationModuleParameters createFactorParameters() {
    return FactorNormalizationModuleParameters.create(List.of(SampleType.QC));
  }

  static @NotNull List<FeatureSelection> toFeatureSelections(
      final @NotNull ModularFeatureListRow... rows) {
    final List<FeatureSelection> selections = new ArrayList<>(rows.length);
    for (final ModularFeatureListRow row : rows) {
      final Double averageMz = row.getAverageMZ();
      if (averageMz == null) {
        throw new IllegalStateException("Row has no average m/z: " + row.getID());
      }

      final Float averageRt = row.getAverageRT();
      if (averageRt == null) {
        throw new IllegalStateException("Row has no average RT: " + row.getID());
      }

      selections.add(new FeatureSelection(Range.singleton(row.getID()), Range.singleton(averageMz),
          Range.singleton(averageRt), null));
    }
    return selections;
  }

  static @NotNull ModularFeature createFeature(final @NotNull ModularFeatureList featureList,
      final @NotNull RawDataFile rawDataFile, final float abundance) {
    return createFeature(featureList, rawDataFile, abundance, abundance);
  }

  static @NotNull ModularFeature createFeature(final @NotNull ModularFeatureList featureList,
      final @NotNull RawDataFile rawDataFile, final float height, final float area) {
    final ModularFeature feature = new ModularFeature(featureList, rawDataFile,
        FeatureStatus.DETECTED);
    feature.setHeight(height);
    feature.setArea(area);
    feature.setMZ(100d);
    feature.setRT(5f);
    return feature;
  }

  static @NotNull ModularFeatureListRow addRow(final @NotNull ModularFeatureList featureList,
      final int rowId, final @NotNull RawDataFile fileA, final @Nullable Float fileAHeight,
      final @Nullable Float fileAArea, final @Nullable RawDataFile fileB,
      final @Nullable Float fileBHeight, final @Nullable Float fileBArea) {
    final ModularFeatureListRow row = new ModularFeatureListRow(featureList, rowId);
    if (fileAHeight != null && fileAArea != null) {
      row.addFeature(fileA, createFeature(featureList, fileA, fileAHeight, fileAArea), false);
    }
    if (fileB != null && fileBHeight != null && fileBArea != null) {
      row.addFeature(fileB, createFeature(featureList, fileB, fileBHeight, fileBArea), false);
    }
    featureList.addRow(row);
    return row;
  }

  static @NotNull ModularFeatureListRow addRow(final @NotNull ModularFeatureList featureList,
      final int rowId, final @NotNull RawDataFile fileA, final @Nullable Float fileAAbundance,
      final @Nullable RawDataFile fileB, final @Nullable Float fileBAbundance) {
    return addRow(featureList, rowId, fileA, fileAAbundance, fileAAbundance, fileB, fileBAbundance,
        fileBAbundance);
  }

  static void addScan(final @NotNull RawDataFileImpl file, final int scanNumber, final int msLevel,
      final float rt, final @NotNull double[] mzValues, final @NotNull double[] intensityValues) {
    final SimpleScan scan = new SimpleScan(file, scanNumber, msLevel, rt, null, mzValues,
        intensityValues, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(0d, 2000d));
    file.addScan(scan);
  }
}
