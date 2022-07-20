/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
    boolean skipRow = false;
    for (Feature f : row.getFeatures()) {
      if(f.getRawDataFile() instanceof ImagingRawDataFile) {
        continue;
      }
      IonTimeSeries<? extends Scan> dpSeries = ((ModularFeature) f).getFeatureData();
      if (dpSeries != null) {
          try {
            ColoredXYDataset dataset = new ColoredXYDataset(
                new IonTimeSeriesToXYProvider((ModularFeature) f), RunOption.THIS_THREAD);
            datasets.add(dataset);
          } catch(NullPointerException npe) {
              skipRow = true;
          }
      }
      if (progress != null) {
        progress.addAndGet(1.0 / size);
      }
    }

    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    final ModularFeature bestFeature = row.getBestFeature();
    final org.jfree.data.Range defaultRange;
    if (bestFeature != null && !skipRow) {
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
