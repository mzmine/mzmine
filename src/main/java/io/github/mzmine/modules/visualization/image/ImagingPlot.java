/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
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
import java.util.logging.Logger;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImagingPlot extends BorderPane {

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
    FeatureImageProvider prov = new FeatureImageProvider(feature,
        parameters.getValue(ImageVisualizerParameters.normalize));
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

  private void setData(ColoredXYZDataset ds) {
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
