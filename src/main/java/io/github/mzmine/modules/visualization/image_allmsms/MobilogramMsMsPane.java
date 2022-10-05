/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
    this.setFillWidth(true);

    tab = new SpectraVisualizerTab(spectrum.getDataFile());
    spectrumPlot = tab.getSpectrumPlot();
    tab.loadRawData(spectrum);

    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    final Color color = ColorUtils.getContrastPaletteColor(feature.getRawDataFile().getColor(),
        palette);

    final IonMobilogramTimeSeries series = (IonMobilogramTimeSeries) feature.getFeatureData();
    final SimpleXYChart<PlotXYDataProvider> mobilogramChart = new SimpleXYChart<>(
        unitFormat.format("Intensity", "a.u."), feature.getMobilityUnit().getAxisLabel());

    mobilogramChart.addDataset(new FeatureRawMobilogramProvider(feature, feature.getRawDataPointsMZRange()));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(series.getSummedMobilogram(), new SimpleObjectProperty<>(
        ColorUtils.getContrastPaletteColor(feature.getRawDataFile().getColor(), palette)),
        FeatureUtils.featureToString(feature)));

    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(feature.getMobilityUnit().getAxisLabel());
    mobilogramChart.setRangeAxisLabel(unitFormat.format("Summed intensity", "a.u."));

    if (spectrum instanceof MergedMsMsSpectrum mergedMsMs) {
      var optMin = mergedMsMs.getSourceSpectra().stream().mapToDouble(s -> ((MobilityScan) s).getMobility()).min();
      var optMax = mergedMsMs.getSourceSpectra().stream().mapToDouble(s -> ((MobilityScan) s).getMobility()).max();
      if (optMin.isPresent() && optMax.isPresent()) {
        IntervalMarker msmsInterval = new IntervalMarker(optMin.getAsDouble(), optMax.getAsDouble(),
            palette.getPositiveColorAWT(), new BasicStroke(1f), palette.getPositiveColorAWT(),
            new BasicStroke(1f), 0.2f);
        mobilogramChart.getXYPlot().addDomainMarker(msmsInterval);
      }
    }

    getChildren().add(mobilogramChart);
    getChildren().add(spectrumPlot);

    spectrumPlot.setMinHeight(250);
    mobilogramChart.setMinHeight(250);

    this.widthProperty().addListener(
        (observable, oldValue, newValue) -> spectrumPlot.getCanvas().widthProperty()
            .set(newValue.doubleValue()));
    this.widthProperty().addListener(
        (observable, oldValue, newValue) -> mobilogramChart.getCanvas().widthProperty()
            .set(newValue.doubleValue()));
  }
}

