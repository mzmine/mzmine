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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.ManualFeatureUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FeatureDataSetCalculationTask extends AbstractTask {

  private final RawDataFile rawDataFile;
  private final List<FeatureDataSet> features;
  private final TICPlot chromPlot;
  private final Scan pseudoScan;
  private final ModularFeature feature;
  private final MZTolerance mzTolerance;
  private final AtomicInteger processedFeatures = new AtomicInteger(0);

  public FeatureDataSetCalculationTask(RawDataFile rawDataFile, TICPlot chromPlot, Scan pseudoScan,
      ModularFeature feature, MZTolerance mzTolerance) {
    super(null, Instant.now());
    this.rawDataFile = rawDataFile;
    this.chromPlot = chromPlot;
    this.pseudoScan = pseudoScan;
    this.feature = feature;
    this.mzTolerance = mzTolerance;
    this.features = new ArrayList<>();
  }

  @Override
  public String getTaskDescription() {
    return "Calculate feature datasets";
  }

  @Override
  public double getFinishedPercentage() {
    return processedFeatures.get() / (double) pseudoScan.getNumberOfDataPoints();
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    ModularFeatureList newFeatureList = new ModularFeatureList("Feature list " + this.hashCode(),
        null, rawDataFile);

    if (getStatus() == TaskStatus.CANCELED) {
      return;
    }

    Range<Float> dataRTRange = Range.closed(feature.getRawDataPointsRTRange().lowerEndpoint(),
        feature.getRawDataPointsRTRange().upperEndpoint());
    List<Double> mzValues = Arrays.stream(pseudoScan.getMzValues(new double[0])).boxed()
        .sorted(Comparator.reverseOrder()).toList();
    for (Double mzValue : mzValues) {
      ManualFeature feature = ManualFeatureUtils.pickFeatureManually(rawDataFile, dataRTRange,
          mzTolerance.getToleranceRange(mzValue), pseudoScan.getMSLevel());
      if (feature != null && feature.getScanNumbers() != null
          && feature.getScanNumbers().length > 0) {
        feature.setFeatureList(newFeatureList);
        ModularFeature modularFeature = FeatureConvertors.ManualFeatureToModularFeature(
            newFeatureList, feature);
        features.add(new FeatureDataSet(modularFeature));
      }
      processedFeatures.getAndIncrement();
    }

    MZmineCore.runLater(() -> {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }
      chromPlot.removeAllFeatureDataSets(false);
      for (FeatureDataSet featureDataSet : features) {
        chromPlot.addFeatureDataSetRandomColor(featureDataSet);
      }
    });

    setStatus(TaskStatus.FINISHED);
  }
}
