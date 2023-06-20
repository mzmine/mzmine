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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.Range;

public class FeatureShapeMobilogramChart extends BufferedChartNode {

  public FeatureShapeMobilogramChart(@NotNull ModularFeatureListRow row, AtomicDouble progress) {
    super(true);
    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    final IMSRawDataFile imsFile = (IMSRawDataFile) row.getRawDataFiles().stream()
        .filter(file -> file instanceof IMSRawDataFile).findAny().orElse(null);
    if (imsFile == null) {
      return;
    }
    final MobilityType mt = imsFile.getMobilityType();
    SimpleXYChart<SummedMobilogramXYProvider> chart = new SimpleXYChart<>(mt.getAxisLabel(),
        uf.format("Intensity", "a.u."));
    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
    chart.setLegendItemsVisible(false);

    Set<ColoredXYDataset> datasets = new LinkedHashSet<>();
    int size = row.getFilesFeatures().size();
    for (Feature f : row.getFeatures()) {
      IonTimeSeries<? extends Scan> series = ((ModularFeature) f).getFeatureData();
      if (series instanceof IonMobilogramTimeSeries) {
        datasets.add(new ColoredXYDataset(new SummedMobilogramXYProvider((ModularFeature) f),
            RunOption.THIS_THREAD));
      }

      if (progress != null) {
        progress.addAndGet(1.0 / size);
      }
    }

    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

    final ModularFeature bestFeature = row.getBestFeature();
    org.jfree.data.Range defaultRange = null;
    if (bestFeature != null && bestFeature.getRawDataFile() instanceof IMSRawDataFile imsRaw) {
      com.google.common.collect.Range<Float> mobilityRange = bestFeature.getMobilityRange();
      final Float mobility = bestFeature.getMobility();
      if (mobilityRange != null && mobility != null && !Float.isNaN(mobility)) {
        final Float length = RangeUtils.rangeLength(mobilityRange);
        defaultRange = new org.jfree.data.Range(
            Math.max(mobility - 3 * length, imsRaw.getDataMobilityRange().lowerEndpoint()),
            Math.min(mobility + 3 * length, imsRaw.getDataMobilityRange().upperEndpoint()));
      }
    }
    if (defaultRange == null) {
      defaultRange = new Range(0, 1);
    }

    chart.addDatasets(datasets);
    chart.getXYPlot().getDomainAxis().setRange(defaultRange);
    chart.getXYPlot().getDomainAxis().setDefaultAutoRange(defaultRange);

    setChartCreateImage(chart, GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH,
        GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
  }
}
