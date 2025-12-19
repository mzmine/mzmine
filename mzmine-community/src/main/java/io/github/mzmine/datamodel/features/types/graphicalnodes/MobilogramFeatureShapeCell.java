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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;

/**
 * Generates a chart at creation with a preferred size set. updateItem sets the datasets in one call
 * so that there is only one call to {@link JFreeChart#fireChartChanged()} and drawing the chart.
 * The first cell seems to be the measurement cell and is never updated here as there are many calls
 * to update item on cell 0 and just single calls on all other cells.
 */
public class MobilogramFeatureShapeCell extends XyChartCell {

  public MobilogramFeatureShapeCell(int id) {
    super(id);
  }

  @Override
  protected int getMinCellHeight() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT;
  }

  @Override
  protected double getMinCellWidth() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

  @Override
  protected void updateItem(Object o, boolean visible) {
    // always need to call super.updateItem
    super.updateItem(o, visible);

    if (!isValidCell() || cellHasNoData()) {
      // datasets removed and plot cleared in XyChartCell.updateItem
      return;
    }

    final ModularFeatureListRow row = getTableRow().getItem();
    final IMSRawDataFile imsFile = (IMSRawDataFile) row.getRawDataFiles().stream()
        .filter(file -> file instanceof IMSRawDataFile).findAny().orElse(null);
    if (imsFile == null) {
      return;
    }
    final MobilityType mt = imsFile.getMobilityType();
    getChart().setDomainAxisLabel(mt.getAxisLabel());

    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    for (final ModularFeature f : row.getFeatures()) {
      IonTimeSeries<? extends Scan> series = f.getFeatureData();
      if (series instanceof IonMobilogramTimeSeries) {
        datasets.add(new DatasetAndRenderer(
            new ColoredXYDataset(new SummedMobilogramXYProvider(f), RunOption.THIS_THREAD),
            new ColoredXYLineRenderer()));
      }
    }

    final org.jfree.data.Range defaultRange;
    final com.google.common.collect.Range<Float> mobilityRange = row.getMobilityRange();
    final Float mobility = row.getAverageMobility();
    if (mobilityRange != null && mobility != null && !Float.isNaN(mobility)) {
      final Float length = RangeUtils.rangeLength(mobilityRange);
      defaultRange = new org.jfree.data.Range(
          Math.max(mobility - 3 * length, imsFile.getDataMobilityRange().lowerEndpoint()),
          Math.min(mobility + 3 * length, imsFile.getDataMobilityRange().upperEndpoint()));
    } else {
      defaultRange = new Range(0, 1);
    }

    getChart().applyWithNotifyChanges(false, () -> {
      try {
        getChart().getXYPlot().getDomainAxis().setRange(defaultRange);
        getChart().getXYPlot().getDomainAxis().setDefaultAutoRange(defaultRange);
      } catch (NullPointerException | NoSuchElementException e) {
        // error in jfreechart draw method
      }
      getChart().setDatasetsAndRenderers(datasets);
    });
  }

  @Override
  protected SimpleXYChart<?> createChart() {
    final UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    // x axis label needs updates in update item()
    SimpleXYChart<SummedMobilogramXYProvider> chart = new SimpleXYChart<>("Mobility",
        uf.format("Intensity", "a.u."));
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
    chart.setLegendItemsVisible(false);
    chart.setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);

    return chart;
  }

}
