package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.LipidAnnotationSummaryType;
import io.github.mzmine.datamodel.features.types.LipidAnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;

public class LipidSpectrumChart extends StackPane {

  public LipidSpectrumChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {

    SimpleXYChart<MassSpectrumProvider> chart =
        new SimpleXYChart<>("Matched fragments", "m/z", "Intensity");
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMZFormat());
    chart.setDefaultRenderer(new ColoredXYBarRenderer(true));
    chart.switchLegendVisible();
    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    List<MatchedLipid> matchedLipids =
        row.get(LipidAnnotationType.class).get(LipidAnnotationSummaryType.class).getValue();

    if (matchedLipids != null & !matchedLipids.isEmpty()) {
      Set<ColoredXYDataset> datasets = new LinkedHashSet<>();
      MatchedLipid match = matchedLipids.get(0);
      Set<LipidFragment> matchedFragments = match.getMatchedFragments();
      Scan matchedMsMsScan =
          matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst().get();
      PlotXYDataProvider spectrumProvider = new MassSpectrumProvider(matchedMsMsScan, "rawSpectrum",
          MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
      ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider);
      datasets.add(spectrumDataSet);

      List<DataPoint> fragmentScanDps =
          matchedFragments.stream().map(LipidFragment::getDataPoint).collect(Collectors.toList());
      PlotXYDataProvider fragmentDataProvider =
          new MassSpectrumProvider(fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
              fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
              "matchedFragments",
              MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT());
      ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider);
      datasets.add(fragmentDataSet);

      Platform.runLater(() -> chart.addDatasets(datasets));
      setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
      getChildren().add(chart);
    }

  }

}
