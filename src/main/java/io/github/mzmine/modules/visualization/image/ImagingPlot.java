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

package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXParameters;
import io.github.mzmine.parameters.ParameterSet;
import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImagingPlot extends BorderPane {

  public static final double[] DEFAULT_IMAGING_QUANTILES = new double[]{0.50, 0.98};
  private static final Logger logger = Logger.getLogger(ImagingPlot.class.getName());
  private final SimpleXYZScatterPlot<FeatureImageProvider> chart;
  private ParameterSet parameters;

  public ImagingPlot(ParameterSet parameters) {
    super();
    this.parameters = parameters;
    chart = createChart();
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
  }

  public void setData(ModularFeature feature) {
    FeatureImageProvider<ImagingScan> prov = new FeatureImageProvider<>(feature,
        (List<ImagingScan>) feature.getFeatureList().getSeletedScans(feature.getRawDataFile()),
        parameters.getValue(ImageVisualizerParameters.imageNormalization));
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);
    setData(ds);
  }

  public void setData(ImagingRawDataFile raw) {
    RawImageProvider prov = new RawImageProvider(raw, parameters);
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.NEW_THREAD);
    setData(ds);
  }

  public void setData(ImagingRawDataFile raw, Range<Double> mzRange) {
    logger.info("Show image with mz range " + mzRange.toString());
    parameters.setParameter(ImageVisualizerParameters.mzRange, mzRange);
    setData(raw);
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

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
//    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralWidth()));
    axis.setVisible(!hideAxes);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class)
        .getParameter(FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    setCenter(chart);
    return chart;
  }

  public SimpleXYZScatterPlot<FeatureImageProvider> getChart() {
    return chart;
  }
}
