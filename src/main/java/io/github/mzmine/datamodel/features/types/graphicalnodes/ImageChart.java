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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXParameters;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageChart extends StackPane {

  private static Logger logger = Logger.getLogger(ImageChart.class.getName());

  public ImageChart(@NotNull ModularFeature f, AtomicDouble progress) {

    FeatureImageProvider prov = new FeatureImageProvider(f);
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);
    // checked in ImagingChart.class

    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");
    ImagingRawDataFile imagingFile = (ImagingRawDataFile) f.getRawDataFile();

    final boolean hideAxes = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.hideImageAxes).getValue();

    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    chart.setDataset(ds);
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setRange(new Range(0, imagingFile.getImagingParam().getLateralHeight()));
    axis.setVisible(!hideAxes);

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
    axis.setRange(new Range(0, imagingFile.getImagingParam().getLateralWidth()));
    axis.setVisible(!hideAxes);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    ImagingParameters param = imagingFile.getImagingParam();

    final double width = lockOnAspectRatio ?
        Math.min(
            GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT / (float) param.getMaxNumberOfPixelY()
                * param.getMaxNumberOfPixelX(), GraphicalColumType.MAXIMUM_GRAPHICAL_CELL_WIDTH)
        : GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH;
    final double height = GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT;

    setPrefHeight(height);
    setPrefWidth(width);
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    BufferedImage img = chart.getChart().createBufferedImage((int) width, (int) height);

    ImageView view = new ImageView(SwingFXUtils.toFXImage(img, null));
    view.setOnMouseClicked(e -> MZmineCore.runLater(() -> {
      getChildren().remove(view);
      getChildren().add(chart);
    }));

    MZmineCore.runLater(() -> getChildren().add(view));
  }

}
