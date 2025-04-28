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
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FeatureShapeIonMobilityRetentionTimeHeatMapChart extends BufferedChartNode {

  public FeatureShapeIonMobilityRetentionTimeHeatMapChart(@NotNull ModularFeature f,
      AtomicDouble progress) {
    super(true);

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

    try {
      chart.getXYPlot().getDomainAxis().setRange(RangeUtils.guavaToJFree(
          RangeUtils.getPositiveRange(dataset.getDomainValueRange(), 0.001d)), false, true);
      chart.getXYPlot().getRangeAxis().setRange(RangeUtils.guavaToJFree(
          RangeUtils.getPositiveRange(dataset.getRangeValueRange(), 0.0001d)), false, true);
    } catch (NullPointerException | NoSuchElementException e) {
      // error in jfreechart draw method
    }

    setChartCreateImage(chart, GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH,
        GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
  }
}
