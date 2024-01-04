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
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
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
    Scan matchedMsMsScan = matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst()
        .orElse(null);
    if (matchedMsMsScan != null) {
      PlotXYDataProvider spectrumProvider = new SingleSpectrumProvider(matchedMsMsScan,
          "MS/MS Spectrum",
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor());
      ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider, runOption);
      addDataSet(spectrumDataSet,
          MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT(), true, null,
          true);
    }
    List<DataPoint> fragmentScanDps = matchedFragments.stream().map(LipidFragment::getDataPoint)
        .toList();
    if (!fragmentScanDps.isEmpty()) {
      PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
          fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
          fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
          "Matched Signals",
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
