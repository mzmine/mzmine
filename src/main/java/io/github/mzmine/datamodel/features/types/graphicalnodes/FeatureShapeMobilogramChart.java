/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.provider.SummedMobilogramXYProvider;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;

public class FeatureShapeMobilogramChart extends StackPane {

  public FeatureShapeMobilogramChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {

    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    final MobilityType mt = ((IMSRawDataFile) row.getRawDataFiles().get(0)).getMobilityType();
    SimpleXYChart<SummedMobilogramXYProvider> chart = new SimpleXYChart<>(
        mt.getAxisLabel(), uf.format("Intensity", "cps"));
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
    chart.switchLegendVisible();

    Set<ColoredXYDataset> datasets = new LinkedHashSet<>();
    int size = row.getFilesFeatures().size();
    for (Feature f : row.getFeatures()) {
      IonTimeSeries<? extends Scan> series = ((ModularFeature) f).getFeatureData();
      if (series instanceof IonMobilogramTimeSeries) {
        datasets.add(new FastColoredXYDataset(new SummedMobilogramXYProvider((ModularFeature) f)));
      }

      if (progress != null) {
        progress.addAndGet(1.0 / size);
      }
    }

    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    getChildren().add(chart);
    Platform.runLater(() -> chart.addDatasets(datasets));
  }
}
