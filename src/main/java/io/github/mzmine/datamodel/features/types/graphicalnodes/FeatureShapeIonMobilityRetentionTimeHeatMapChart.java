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
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonMobilogramTimeSeriesToRtMobilityHeatmapProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FeatureShapeIonMobilityRetentionTimeHeatMapChart extends StackPane {

  public FeatureShapeIonMobilityRetentionTimeHeatMapChart(@NotNull ModularFeature f,
      AtomicDouble progress) {

    SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> chart = new SimpleXYZScatterPlot<>();
    ColoredXYZDataset dataset = new ColoredXYZDataset(
        new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(f), RunOption.THIS_THREAD);
    MobilityType mt = ((IMSRawDataFile) f.getRawDataFile()).getMobilityType();
    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    chart.setRangeAxisLabel(mt.getAxisLabel());
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
    chart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    chart.setLegendNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    chart.setDataset(dataset);
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeMinimumSize(0.005);
    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setPrefWidth(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH);
    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));

    // todo: save min/max values of dataset in dataset iself so jfreechart does not have to loop
    //  over all data points (also means the renderers have to support it)
    chart.getXYPlot().getDomainAxis()
        .setRange(RangeUtils.guavaToJFree(RangeUtils.getPositiveRange(dataset.getDomainValueRange(), 0.001d)), false, true);
    chart.getXYPlot().getRangeAxis()
        .setRange(RangeUtils.guavaToJFree(RangeUtils.getPositiveRange(dataset.getRangeValueRange(), 0.0001d)), false, true);
    BufferedImage img = chart.getChart()
        .createBufferedImage(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH,
            GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);

    ImageView view = new ImageView(SwingFXUtils.toFXImage(img, null));
    view.setOnMouseClicked(e -> MZmineCore.runLater(() -> {
      // change buffered image to buffered chart on mouse click
      getChildren().remove(view);
      getChildren().add(chart);
    }));

    Platform.runLater(() -> getChildren().add(view));

//    chart.addDatasetsChangedListener(
//        (e) -> Platform.runLater(() -> chart.getXYPlot().getRangeAxis().setAutoRange(true)));
//    getChildren().add(chart);
//    Platform.runLater(() -> chart.setDataset(dataset));
  }
}
