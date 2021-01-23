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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.graphicalnodes.provider.IonMobilityTimeSeriesXYZProvider;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYZDataset;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import org.jfree.chart.axis.NumberAxis;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FeatureShapeIonMobilityRetentionTimeHeatMapChart extends StackPane {

  public FeatureShapeIonMobilityRetentionTimeHeatMapChart(@Nonnull ModularFeature f,
      AtomicDouble progress) {

    SimpleXYZScatterPlot<IonMobilityTimeSeriesXYZProvider> chart = new SimpleXYZScatterPlot<>();
    ColoredXYZDataset dataset = new FastColoredXYZDataset(new IonMobilityTimeSeriesXYZProvider(f));
    MobilityType mt = ((IMSRawDataFile) f.getRawDataFile()).getMobilityType();
    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    chart.setRangeAxisLabel(mt.getAxisLabel());
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
    chart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    chart.setLegendNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeMinimumSize(0.005);
    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);
    chart.addDatasetsChangedListener(
        (e) -> Platform.runLater(() -> chart.getXYPlot().getRangeAxis().setAutoRange(true)));
    getChildren().add(chart);

    Platform.runLater(() -> chart.setDataset(dataset));
  }
}
