/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonMobilogramTimeSeriesToRtMobilityHeatmapProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import org.jfree.chart.axis.NumberAxis;

/**
 * Currently not used because the dataset needs computation on another thread anyway.
 */
@Deprecated
public class ImsHeatmapChartCell extends ChartCell<SimpleXYZScatterPlot<?>> {

  private final RawDataFile file;

  public ImsHeatmapChartCell(int id, final RawDataFile file) {
    this.file = file;
    super(id);
  }

  @Override
  protected void updateItem(Object o, boolean b) {
    super.updateItem(o, b);

    if (!isValidCell()) {
      return;
    }
    // remove crosshair, determined by cursor position in FxXYPlot
    plot.setCursorPosition(null);
    // clear zoom history because it comes from old data
    plot.getZoomHistory().clear();
    plot.removeAllDatasets();

    System.out.println(
        "imts " + getTableColumn().getWidth() + " " + getWidth() + " " + getMinWidth());

    if (cellHasNoData()) {
      return;
    }

    final ModularFeatureListRow row = getTableRow().getItem();
    ModularFeature feature = row.getFeature(file);
    if (feature != null && feature.getFeatureData() instanceof IonMobilogramTimeSeries) {
      getChart().setVisible(true);
      ColoredXYZDataset dataset = new ColoredXYZDataset(
          new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(feature), RunOption.THIS_THREAD);
      getChart().getXYPlot().getDomainAxis()
          .setRange(RangeUtils.guavaToJFree(feature.getRawDataPointsRTRange()));
      getChart().getXYPlot().getRangeAxis()
          .setRange(RangeUtils.guavaToJFree(feature.getMobilityRange()));
      getChart().setDataset(dataset);
    } else {
      getChart().setVisible(false);
    }
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
  protected SimpleXYZScatterPlot<?> createChart() {
    final SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> chart = new SimpleXYZScatterPlot<>();
    MobilityType mt = ((IMSRawDataFile) file).getMobilityType();
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
    chart.setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    chart.setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);
    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    return chart;
  }
}
