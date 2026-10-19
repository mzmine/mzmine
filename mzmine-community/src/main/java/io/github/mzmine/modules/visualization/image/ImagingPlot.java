/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXParameters;
import java.awt.Color;
import java.util.List;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleInsets;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImagingPlot extends BorderPane {

  public static final double[] DEFAULT_IMAGING_QUANTILES = new double[]{0.50, 0.98};
  private final SimpleXYZScatterPlot<FeatureImageProvider> chart;

  /**
   * Creates an imaging plot that normalizes images like the feature table, following the image
   * normalization of the preferences.
   */
  public ImagingPlot() {
    super();
    chart = createChart();
  }

  public void setData(Feature feature) {
    FeatureImageProvider<ImagingScan> prov = new FeatureImageProvider<>(feature,
        (List<ImagingScan>) feature.getFeatureList().getSeletedScans(feature.getRawDataFile()),
        MZmineCore.getConfiguration().getImageNormalization());
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);
    setData(ds);
  }

  public void setData(ColoredXYZDataset ds) {
    chart.setDataset(ds);
  }

  private SimpleXYZScatterPlot<FeatureImageProvider> createChart() {
    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");

    final boolean hideAxes = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class)
        .getParameter(FeatureTableFXParameters.hideImageAxes).getValue();

    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
//    chart.setDataset(ds);
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
//    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralHeight()));
    axis.setVisible(!hideAxes);
    axis.setLowerMargin(0);
    axis.setUpperMargin(0);

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
//    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralWidth()));
    axis.setVisible(!hideAxes);
    axis.setLowerMargin(0);
    axis.setUpperMargin(0);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class)
        .getParameter(FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    chart.setLegendVisible(true);

    chart.getXYPlot().setAxisOffset(RectangleInsets.ZERO_INSETS);
    chart.setShowCrosshair(true);

    setCenter(chart);
    return chart;
  }

  public SimpleXYZScatterPlot<FeatureImageProvider> getChart() {
    return chart;
  }
}
