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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYDatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.SingleSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Ideally refactor this away from the spectrum plot, as it does not consistently use the
 * {@link io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot}.
 */
@Deprecated
public class LipidSpectrumPlot extends SpectraPlot {

  public LipidSpectrumPlot(MatchedLipid matchedLipid, boolean showLegend, RunOption runOption) {
    super(false, showLegend);

    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);

    getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    updateLipidSpectrum(matchedLipid, showLegend, runOption);
  }

  public void updateLipidSpectrum(@Nullable MatchedLipid matchedLipid, boolean showLegend,
      RunOption runOption) {

    if (matchedLipid == null) {
      clearPlot();
      return;
    }

    final List<LipidFragment> matchedFragments = new ArrayList<>(
        matchedLipid.getMatchedFragments());
    final Scan matchedMsMsScan = matchedFragments.stream().map(LipidFragment::getMsMsScan)
        .findFirst().orElse(null);

    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    final List<XYDatasetAndRenderer> datasets = new ArrayList<>();
    if (matchedMsMsScan != null) {
      PlotXYDataProvider spectrumProvider = new SingleSpectrumProvider(matchedMsMsScan,
          "MS/MS Spectrum", palette.getNegativeColor());
      ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider, runOption);
      datasets.add(new DatasetAndRenderer(spectrumDataSet, new ColoredXYBarRenderer(true)));
    }

    List<DataPoint> fragmentScanDps = matchedFragments.stream().map(LipidFragment::getDataPoint)
        .toList();
    if (!fragmentScanDps.isEmpty()) {
      final PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
          fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
          fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
          "Matched Signals", palette.getPositiveColorAWT());
      final ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider,
          runOption);
      final MatchedLipidLabelGenerator matchedLipidLabelGenerator = new MatchedLipidLabelGenerator(
          this, matchedFragments);
      final ColoredXYBarRenderer matchedRenderer = new ColoredXYBarRenderer(true);

      matchedRenderer.setDefaultItemLabelsVisible(true);
      matchedRenderer.setSeriesItemLabelGenerator(1, matchedLipidLabelGenerator);
      datasets.add(new DatasetAndRenderer(fragmentDataSet, matchedRenderer));
    }

    applyWithNotifyChanges(false, () -> {
      clearPlot();
      getXYPlot().setDatasetsRenderers(datasets);
      setLegendVisible(showLegend);
      addPrecursorMarkers(matchedMsMsScan);
    });
  }

  public void clearPlot() {
    getXYPlot().removeAllDatasets();
    getChart().getXYPlot().clearDomainMarkers();
  }
}
