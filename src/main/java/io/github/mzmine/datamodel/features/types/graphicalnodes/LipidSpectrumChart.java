/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidSpectrumChart extends StackPane {

  private SpectraPlot spectraPlot;

  public LipidSpectrumChart(@Nullable MatchedLipid match, AtomicDouble progress,
      RunOption runOption) {
    if (match == null || match.getMatchedFragments() == null || match.getMatchedFragments()
        .isEmpty()) {
      return;
    }

    spectraPlot = new SpectraPlot();
    spectraPlot.setPrefHeight(GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT);

    List<LipidFragment> matchedFragments = new ArrayList<>(match.getMatchedFragments());
    Scan matchedMsMsScan = matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst()
        .orElse(null);
    if (matchedMsMsScan != null) {
      PlotXYDataProvider spectrumProvider = new LipidSpectrumProvider(null, matchedMsMsScan,
          "MS/MS Spectrum",
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT());
      ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider, runOption);
      spectraPlot.addDataSet(spectrumDataSet,
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT(), true, null,
          true);
    }

    List<DataPoint> fragmentScanDps = matchedFragments.stream().map(LipidFragment::getDataPoint)
        .collect(Collectors.toList());
    if (!fragmentScanDps.isEmpty()) {
      PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
          fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
          fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
          "Matched Signals",
          MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
      ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider, runOption);
      MatchedLipidLabelGenerator matchedLipidLabelGenerator = new MatchedLipidLabelGenerator(
          spectraPlot, matchedFragments);
      spectraPlot.getXYPlot().getRenderer().setDefaultItemLabelsVisible(true);
      spectraPlot.getXYPlot().getRenderer()
          .setSeriesItemLabelGenerator(1, matchedLipidLabelGenerator);
      spectraPlot.addDataSet(fragmentDataSet,
          MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), true,
          matchedLipidLabelGenerator, true);
    }

    MZmineCore.runLater(() -> {
      getChildren().add(spectraPlot);
    });
  }

  public LipidSpectrumChart(@NotNull ModularFeatureListRow row, AtomicDouble progress) {
    this(row.getLipidMatches().isEmpty() ? null : row.getLipidMatches().get(0), progress,
        RunOption.NEW_THREAD);
  }

  public SpectraPlot getSpectraPlot() {
    return spectraPlot;
  }
}
