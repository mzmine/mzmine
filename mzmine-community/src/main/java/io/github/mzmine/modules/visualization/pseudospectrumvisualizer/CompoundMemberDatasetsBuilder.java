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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.main.ConfigService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates the chromatogram datasets of a pseudo spectrum from the member features of its compound.
 * The features of a GC-EI deconvolution are kept as rows of a compound, so the chromatograms are
 * not extracted again from raw data.
 */
final class CompoundMemberDatasetsBuilder {

  private CompoundMemberDatasetsBuilder() {
  }

  /**
   * @return the compound of this row if the feature list has a valid compound list
   */
  static @Nullable CompoundRow findCompound(@NotNull final FeatureListRow row) {
    if (row instanceof CompoundRow compound) {
      return compound;
    }
    final FeatureList flist = row.getFeatureList();
    if (flist == null || !flist.hasCompoundList()) {
      return null;
    }
    final CompoundList compoundList = flist.getCompoundList();
    return compoundList == null ? null : compoundList.findFirstCompoundOf(row).orElse(null);
  }

  /**
   * The representative feature is drawn as area and all other member features as lines.
   *
   * @param compound the compound with all features of the pseudo spectrum
   * @param dataFile the sample to draw
   * @param color    color of all chromatograms
   * @return the datasets, empty if the representative has no feature in this sample
   */
  static @NotNull List<DatasetAndRenderer> createDatasets(@NotNull final CompoundRow compound,
      @NotNull final RawDataFile dataFile, @NotNull final Color color) {
    final FeatureListRow representative = compound.getPreferredRow();
    final Feature representativeFeature = representative.getFeature(dataFile);
    if (representativeFeature == null || representativeFeature.getFeatureData() == null) {
      return List.of();
    }

    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    datasets.add(new DatasetAndRenderer(
        createDataset(representativeFeature.getFeatureData(), representativeFeature, color), new ColoredAreaShapeRenderer()));

    final List<Feature> members = compound.getMemberRows().stream()
        .filter(row -> row != representative).map(row -> row.getFeature(dataFile))
        .filter(f -> f != null && f.getFeatureData() != null)
        .sorted(Comparator.comparingDouble(Feature::getMZ)).toList();
    for (final Feature member : members) {
      datasets.add(
          new DatasetAndRenderer(createDataset(member.getFeatureData(), member, color),
              new ColoredXYLineRenderer()));
    }
    return datasets;
  }

  private static @NotNull ColoredXYDataset createDataset(
      @NotNull final IonTimeSeries<?> series, @NotNull final Feature feature,
      @NotNull final Color color) {
    final String label = ConfigService.getGuiFormats().mz(feature.getMZ());
    return new ColoredXYDataset(new IonTimeSeriesToXYProvider(series, label, color),
        RunOption.THIS_THREAD);
  }
}
