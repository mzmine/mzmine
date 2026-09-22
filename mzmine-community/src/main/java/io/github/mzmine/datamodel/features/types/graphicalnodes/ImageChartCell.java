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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.beust.jcommander.internal.Nullable;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

/**
 * Generates a chart at creation with a preferred size set. updateItem sets the datasets in one call
 * so that there is only one call to {@link JFreeChart#fireChartChanged()} and drawing the chart.
 * The first cell seems to be the measurement cell and is never updated here as there are many calls
 * to update item on cell 0 and just single calls on all other cells.
 */
public class ImageChartCell extends ChartCell<SimpleXYZScatterPlot<?>> {

  private final RawDataFile file;

  public ImageChartCell(int id, @Nullable RawDataFile file) {
    super(id);
    this.file = file;
  }

  @Override
  protected int getMinCellHeight() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT;
  }

  @Override
  protected double getMinCellWidth() {
    return GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH;
  }

  @Override
  protected void updateItem(Object o, boolean visible) {
    // always need to call super.updateItem
    super.updateItem(o, visible);

    if (!isValidCell() || cellHasNoData() || !(file instanceof ImagingRawDataFile imgFile)) {
      // datasets removed and plot cleared in XyChartCell.updateItem
      return;
    }

    final ModularFeatureListRow row = getTableRow().getItem();
    final ModularFeature feature = row.getFeature(imgFile);
    if (feature == null) {
      return;
    }

    final FeatureImageProvider<ImagingScan> prov = new FeatureImageProvider<>(feature);
    final ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class)
        .getParameter(FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    ImagingParameters param = imgFile.getImagingParam();
    final double width = lockOnAspectRatio ? Math.min(
        GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT / (float) param.getMaxNumberOfPixelY()
            * param.getMaxNumberOfPixelX(), GraphicalColumType.MAXIMUM_GRAPHICAL_CELL_WIDTH)
        : GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH;

    getChart().applyWithNotifyChanges(false, true, () -> {
      getChart().setDataset(ds);

      getChart().setMinWidth(width);
      getChart().setMinHeight(GraphicalColumType.DEFAULT_IMAGE_CELL_HEIGHT);
      final XYPlot xyplot = getChart().getXYPlot();
      Range rangeAxisRange = new Range(0, param.getLateralHeight());
      Range domainAxisRange = new Range(0, param.getLateralWidth());

      NumberAxis axis = (NumberAxis) xyplot.getRangeAxis();
      axis.setRange(rangeAxisRange);
      axis.setDefaultAutoRange(rangeAxisRange);
      axis = (NumberAxis) xyplot.getDomainAxis();
      axis.setRange(domainAxisRange);
      axis.setDefaultAutoRange(domainAxisRange);
    });
  }

  @Override
  protected SimpleXYZScatterPlot<?> createChart() {
    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");
    final boolean hideAxes = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class)
        .getParameter(FeatureTableFXParameters.hideImageAxes).getValue();

    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setVisible(!hideAxes);

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);

    axis.setVisible(!hideAxes);
    chart.setLegendVisible(!hideAxes);
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    return chart;
  }

}
