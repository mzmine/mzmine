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
 *
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageChart extends StackPane {

  private static Logger logger = Logger.getLogger(ImageChart.class.getName());
//  private Double dataPointWidth;
//  private Double dataPointHeight;
//  private PaintScale paintScaleParameter;

  public ImageChart(@Nonnull ModularFeature f, AtomicDouble progress) {

    FeatureImageProvider prov = new FeatureImageProvider(f);
    FastColoredXYZDataset ds = new FastColoredXYZDataset(prov);
    // checked in ImagingChart.class

    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");
    ImagingRawDataFile imagingFile = (ImagingRawDataFile) f.getRawDataFile();
    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setRange(new Range(0, imagingFile.getImagingParam().getLateralHeight()));

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
    axis.setRange(new Range(0, imagingFile.getImagingParam().getLateralWidth()));

    setPrefHeight(200);
    setPrefWidth(200);
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    BufferedImage img = chart.getChart()
        .createBufferedImage(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH + 50,
            GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);

    ImageView view = new ImageView(SwingFXUtils.toFXImage(img, null));
    view.setOnMouseClicked(e -> MZmineCore.runLater(() -> {
      getChildren().remove(view);
      getChildren().add(chart);
    }));

    Platform.runLater(() -> getChildren().add(view));

    /*Platform.runLater(() -> {
      getChildren().add(chart);
      chart.setDataset(ds);
    });*/

  }

}
