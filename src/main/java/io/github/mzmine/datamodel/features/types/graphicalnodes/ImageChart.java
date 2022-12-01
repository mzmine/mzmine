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
import io.github.mzmine.datamodel.ImagingScan;
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
    FeatureImageProvider<ImagingScan> prov = new FeatureImageProvider<>(f);
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
