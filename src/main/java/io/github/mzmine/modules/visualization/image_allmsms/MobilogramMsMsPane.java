package io.github.mzmine.modules.visualization.image_allmsms;

import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.FeatureRawMobilogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.IntervalMarker;

public class MobilogramMsMsPane extends VBox {

  private final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
  private final SpectraPlot spectrumPlot;
  private final SpectraVisualizerTab tab;

  public MobilogramMsMsPane(@NotNull final Scan spectrum, Feature feature) {
    tab = new SpectraVisualizerTab(spectrum.getDataFile());
    spectrumPlot = tab.getSpectrumPlot();
    tab.loadRawData(spectrum);

    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    final Color color = ColorUtils.getContrastPaletteColor(feature.getRawDataFile().getColor(),
        palette);

    final IonMobilogramTimeSeries series = (IonMobilogramTimeSeries) feature.getFeatureData();
    final SimpleXYChart<PlotXYDataProvider> mobilogramChart = new SimpleXYChart<>(
        unitFormat.format("Intensity", "a.u."), feature.getMobilityUnit().getAxisLabel());

    mobilogramChart.addDataset(
        new FeatureRawMobilogramProvider(feature, feature.getRawDataPointsMZRange()));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(series.getSummedMobilogram(),
        new SimpleObjectProperty<>(
            ColorUtils.getContrastPaletteColor(feature.getRawDataFile().getColor(), palette)),
        FeatureUtils.featureToString(feature)));

    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(feature.getMobilityUnit().getAxisLabel());
    mobilogramChart.setRangeAxisLabel(unitFormat.format("Summed intensity", "a.u."));

    if (spectrum instanceof MergedMsMsSpectrum mergedMsMs) {
      var optMin = mergedMsMs.getSourceSpectra().stream()
          .mapToDouble(s -> ((MobilityScan) s).getMobility()).min();
      var optMax = mergedMsMs.getSourceSpectra().stream()
          .mapToDouble(s -> ((MobilityScan) s).getMobility()).max();
      if (optMin.isPresent() && optMax.isPresent()) {
        IntervalMarker msmsInterval = new IntervalMarker(optMin.getAsDouble(), optMax.getAsDouble(),
            palette.getPositiveColorAWT(), new BasicStroke(1f), palette.getPositiveColorAWT(),
            new BasicStroke(1f), 0.2f);
        mobilogramChart.getXYPlot().addDomainMarker(msmsInterval);
      }
    }

    getChildren().add(spectrumPlot);
    getChildren().add(mobilogramChart);
  }
}

