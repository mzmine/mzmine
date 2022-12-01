/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.Range;

public class FeatureShapeChart extends StackPane {


  public FeatureShapeChart(@NotNull ModularFeatureListRow row, AtomicDouble progress) {

    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    SimpleXYChart<IonTimeSeriesToXYProvider> chart = new SimpleXYChart<>(
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    chart.setLegendItemsVisible(false);

    Set<ColoredXYDataset> datasets = new LinkedHashSet<>();
    int size = row.getFilesFeatures().size();
    for (Feature f : row.getFeatures()) {
      if(f.getRawDataFile() instanceof ImagingRawDataFile) {
        continue;
      }
      IonTimeSeries<? extends Scan> dpSeries = ((ModularFeature) f).getFeatureData();
      if (dpSeries != null) {
        ColoredXYDataset dataset = new ColoredXYDataset(
            new IonTimeSeriesToXYProvider((ModularFeature) f), RunOption.THIS_THREAD);
        datasets.add(dataset);
      }
      if (progress != null) {
        progress.addAndGet(1.0 / size);
      }
    }

    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    final ModularFeature bestFeature = row.getBestFeature();
    final org.jfree.data.Range defaultRange;
    if (bestFeature != null) {
      final Float rt = bestFeature.getRT();

      if (bestFeature.getFWHM() != null && !Float.isNaN(bestFeature.getFWHM())
          && bestFeature.getFWHM() > 0f) {
        final Float fwhm = bestFeature.getFWHM();
        defaultRange = new org.jfree.data.Range(Math.max(rt - 5 * fwhm, 0),
            Math.min(rt + 5 * fwhm, bestFeature.getRawDataFile().getDataRTRange().upperEndpoint()));

      } else {
        final Float length = Math.max(RangeUtils.rangeLength(bestFeature.getRawDataPointsRTRange()),
            0.001f);
        defaultRange = new org.jfree.data.Range(Math.max(rt - 3 * length, 0),
            Math.min(rt + 3 * length,
                bestFeature.getRawDataFile().getDataRTRange().upperEndpoint()));
      }
    } else {
      defaultRange = new Range(0, 1);
    }

    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    Platform.runLater(() -> {
      getChildren().add(chart);
      chart.addDatasets(datasets);

      chart.getXYPlot().getDomainAxis().setRange(defaultRange);
      chart.getXYPlot().getDomainAxis().setDefaultAutoRange(defaultRange);
    });
  }
}
