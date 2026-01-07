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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.SingleSpectrumProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class LipidSpectrumPlot extends SpectraPlot {

  public LipidSpectrumPlot(MatchedLipid matchedLipid, boolean showLegend, RunOption runOption) {
    super(false, showLegend);

    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);
    List<LipidFragment> matchedFragments = new ArrayList<>(matchedLipid.getMatchedFragments());
    Scan matchedMsMsScan = matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst().orElse(null);
    if (matchedMsMsScan != null) {
      PlotXYDataProvider spectrumProvider = new SingleSpectrumProvider(matchedMsMsScan,
          "MS/MS Spectrum",
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor());
      ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider, runOption);
      addDataSet(spectrumDataSet,
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT(), true, null,
          true);
    }
    List<DataPoint> fragmentScanDps = matchedFragments.stream().map(LipidFragment::getDataPoint).toList();
    if (!fragmentScanDps.isEmpty()) {
      PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
          fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
          fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(), "Matched Signals",
          MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
      ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider, runOption);
      MatchedLipidLabelGenerator matchedLipidLabelGenerator = new MatchedLipidLabelGenerator(this,
          matchedFragments);
      getXYPlot().getRenderer().setDefaultItemLabelsVisible(true);
      getXYPlot().getRenderer().setSeriesItemLabelGenerator(1, matchedLipidLabelGenerator);
      addDataSet(fragmentDataSet,
          MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), true,
          matchedLipidLabelGenerator, true);
      setLegendVisible(showLegend);
      addPrecursorMarkers(matchedMsMsScan);
    }
    getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));
  }
}
