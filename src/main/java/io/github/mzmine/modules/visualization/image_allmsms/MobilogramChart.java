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

package io.github.mzmine.modules.visualization.image_allmsms;

import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.Collection;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.IntervalMarker;

public class MobilogramChart extends BorderPane {

  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
  private final SimpleXYChart<PlotXYDataProvider> mobilogramChart;
  private final boolean normalize;

  public MobilogramChart() {
    this(null);
  }

  public MobilogramChart(@Nullable final Feature feature) {
    this(feature, false);
  }

  public MobilogramChart(@Nullable final Feature feature, final boolean normalize) {
    mobilogramChart = new SimpleXYChart<>("Mobility",
        normalize ? "Normalized intensity" : unitFormat.format("Summed intensity", "a.u."));

    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    if (!normalize) {
      mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    }

    setCenter(mobilogramChart);
    mobilogramChart.setMinHeight(200);
    this.widthProperty().addListener(
        (observable, oldValue, newValue) -> mobilogramChart.getCanvas().widthProperty()
            .set(newValue.doubleValue()));
    this.normalize = normalize;

    if (feature != null) {
      addFeature(feature);
    }

    mobilogramChart.getChart().removeLegend();
  }

  public void addFeature(@NotNull Feature feature) {
    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

    if (feature.getFeatureData() instanceof IonMobilogramTimeSeries series) {
//      mobilogramChart.addDataset(
//          new FeatureRawMobilogramProvider(feature, feature.getRawDataPointsMZRange()));
      mobilogramChart.setDomainAxisLabel(feature.getMobilityUnit().getAxisLabel());
      mobilogramChart.addDataset(new SummedMobilogramXYProvider(series.getSummedMobilogram(),
          new SimpleObjectProperty<>(
              ColorUtils.getContrastPaletteColor(feature.getRawDataFile().getColor(), palette)),
          FeatureUtils.featureToString(feature), false, normalize, null));
      mobilogramChart.setDomainAxisLabel(feature.getMobilityUnit().getAxisLabel());
    }

    if (feature.getMostIntenseFragmentScan() instanceof MergedMsMsSpectrum mergedMsMs) {
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
  }

  public void addProviders(Collection<PlotXYDataProvider> providers) {
    mobilogramChart.addDatasetProviders(providers);
  }
}
