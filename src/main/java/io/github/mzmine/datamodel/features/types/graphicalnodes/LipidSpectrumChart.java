package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.LipidAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.LipidAnnotationSummaryType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.LipidSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class LipidSpectrumChart extends StackPane {

  public LipidSpectrumChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {

    SimpleXYChart<LipidSpectrumProvider> chart =
        new SimpleXYChart<>("Matched fragments", "m/z", "Intensity");
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMZFormat());
    chart.setDefaultRenderer(new ColoredXYBarRenderer(true));
    chart.setLegendItemsVisible(true);
    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    List<MatchedLipid> matchedLipids =
        row.get(LipidAnnotationType.class).get(LipidAnnotationSummaryType.class).getValue();
    if (matchedLipids != null && !matchedLipids.isEmpty()) {
      List<ColoredXYDataset> datasets = new ArrayList<>();
      MatchedLipid match = matchedLipids.get(0);
      List<LipidFragment> matchedFragments = new ArrayList<>();
      if (match.getMatchedFragments() != null && !match.getMatchedFragments().isEmpty()) {
        matchedFragments.addAll(match.getMatchedFragments());
        Scan matchedMsMsScan =
            matchedFragments.stream().map(LipidFragment::getMsMsScan).findFirst().get();
        if (matchedMsMsScan != null) {
          PlotXYDataProvider spectrumProvider =
              new LipidSpectrumProvider(null, matchedMsMsScan, "MS/MS Spectrum",
                  MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
          ColoredXYDataset spectrumDataSet = new ColoredXYDataset(spectrumProvider);
          datasets.add(spectrumDataSet);
        }

        List<DataPoint> fragmentScanDps =
            matchedFragments.stream().map(LipidFragment::getDataPoint).collect(Collectors.toList());
        if (fragmentScanDps != null && !fragmentScanDps.isEmpty()) {
          PlotXYDataProvider fragmentDataProvider = new LipidSpectrumProvider(matchedFragments,
              fragmentScanDps.stream().mapToDouble(DataPoint::getMZ).toArray(),
              fragmentScanDps.stream().mapToDouble(DataPoint::getIntensity).toArray(),
              "Matched Signals",
              MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT());
          ColoredXYDataset fragmentDataSet = new ColoredXYDataset(fragmentDataProvider);
          datasets.add(fragmentDataSet);
        }
      }

      // Add label generator for the dataset
      MatchedLipidLabelGenerator matchedLipidLabelGenerator =
          new MatchedLipidLabelGenerator(matchedFragments);
      chart.getXYPlot().getRenderer().setSeriesItemLabelGenerator(
          chart.getXYPlot().getSeriesCount(), matchedLipidLabelGenerator);

      Platform.runLater(() -> chart.addDatasets(datasets));
      setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
      getChildren().add(chart);
    }

  }

  class MatchedLipidLabelGenerator implements XYItemLabelGenerator {

    public static final int POINTS_RESERVE_X = 100;

    private List<LipidFragment> fragments;

    public MatchedLipidLabelGenerator(List<LipidFragment> fragments) {
      this.fragments = fragments;
    }

    @Override
    public String generateLabel(XYDataset dataset, int series, int item) {


      // Create label
      String label = null;
      if (dataset.getSeriesKey(1).equals("Matched Signals")) {
        if (fragments != null) {
          return buildFragmentAnnotation(fragments.get(item));
        } else {
          return null;
        }
      }
      return label;
    }

  }

  private String buildFragmentAnnotation(LipidFragment lipidFragment) {
    if (lipidFragment.getLipidFragmentInformationLevelType()
        .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
      StringBuilder sb = new StringBuilder();
      sb.append(lipidFragment.getLipidChainType() + " " + lipidFragment.getChainLength() + ":"
          + lipidFragment.getNumberOfDBEs());
      System.out.println(sb.toString());
      return sb.toString();
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(lipidFragment.getRuleType());
      return sb.toString();
    }
  }

}
