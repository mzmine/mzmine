/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.Range;

public class FeatureShapeChart extends BufferedChartNode {

  public FeatureShapeChart(@NotNull ModularFeatureListRow row, AtomicDouble progress) {
    super(true);
    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    SimpleXYChart<IonTimeSeriesToXYProvider> chart = new SimpleXYChart<>(
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    chart.setLegendItemsVisible(false);

    List<ColoredXYDataset> datasets = new ArrayList<>();
    int size = row.getFilesFeatures().size();
    for (ModularFeature f : row.getFeatures()) {
      if (f.getRawDataFile() instanceof ImagingRawDataFile) {
        continue;
      }
      IonTimeSeries<? extends Scan> dpSeries = f.getFeatureData();
      if (dpSeries != null) {
        ColoredXYDataset dataset = new ColoredXYDataset(new IonTimeSeriesToXYProvider(f),
            RunOption.THIS_THREAD);
        datasets.add(dataset);
      }
      if (progress != null) {
        progress.addAndGet(1.0 / size);
      }
    }

    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    final ModularFeature bestFeature = row.getBestFeature();
    final org.jfree.data.Range defaultRange;
    if (bestFeature != null) {
      final Float rt = bestFeature.getRT();

      var fwhm = bestFeature.getFWHM();
      var fullWidth = RangeUtils.rangeLength(bestFeature.getRawDataPointsRTRange());
      var dataRTRange = bestFeature.getRawDataFile().getDataRTRange();
      var rawMinRt = dataRTRange.lowerEndpoint();
      var rawMaxRt = dataRTRange.upperEndpoint();
      // FWHM defines most of the feature / chromatogram
      if (fwhm != null && !Float.isNaN(fwhm) && fwhm > 0f && fwhm / fullWidth > 0.4) {
        // zoom on feature
        var window = 5 * fwhm;
        defaultRange = new org.jfree.data.Range(Math.max(rt - window, rawMinRt),
            Math.min(rt + window, rawMaxRt));
      } else {
        // show full RT range
        final float length = Math.max(fullWidth, 0.001f);
        defaultRange = new org.jfree.data.Range(Math.max(rt - length * 1.05, rawMinRt),
            Math.min(rt + length * 1.05, rawMaxRt));
      }
    } else {
      defaultRange = new Range(0, 1);
    }

    chart.addDatasets(datasets);
    try {
      chart.getXYPlot().getDomainAxis().setRange(defaultRange);
      chart.getXYPlot().getDomainAxis().setDefaultAutoRange(defaultRange);
    } catch (NullPointerException | NoSuchElementException ex) {
      // error in jfreechart draw method
    }

    // set the chart to create a buffered image
    setChartCreateImage(chart, GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH,
        GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
  }
}
