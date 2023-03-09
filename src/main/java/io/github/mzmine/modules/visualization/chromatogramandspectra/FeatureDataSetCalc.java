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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.FeatureTICRenderer;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.ManualFeatureUtils;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Calculates The feature data sets in a new thread to safe perfomance and not make the gui freeze.
 * Nested class because it uses the {@link ChromatogramAndSpectraVisualizer#chromPlot} member.
 */
public class FeatureDataSetCalc extends AbstractTask {

  public static final Logger logger = Logger.getLogger(FeatureDataSetCalc.class.getName());

  private final NumberFormat mzFormat;
  private final Collection<RawDataFile> rawDataFiles;
  private final Range<Double> mzRange;
  private int doneFiles;
  private final List<FeatureDataSet> features;
  private final HashMap<FeatureDataSet, FeatureTICRenderer> dataSetsAndRenderers;
  private final TICPlot chromPlot;
  private final ScanSelection scanSelection;

  public FeatureDataSetCalc(final Collection<RawDataFile> rawDataFiles, final Range<Double> mzRange,
      ScanSelection scanSelection, TICPlot chromPlot) {
    super(null, Instant.now()); // no new data stored -> null, date irrelevant (not used in batch)
    this.rawDataFiles = rawDataFiles;
    this.mzRange = mzRange;
    this.chromPlot = chromPlot;
    this.scanSelection = scanSelection;
    doneFiles = 0;
    setStatus(TaskStatus.WAITING);
    features = new ArrayList<>();
    dataSetsAndRenderers = new HashMap<>();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
  }

  @Override
  public String getTaskDescription() {
    return "Calculating base peak chromatogram(s) of m/z " + mzFormat.format(
        (mzRange.upperEndpoint() + mzRange.lowerEndpoint()) / 2) + " in " + rawDataFiles.size()
        + " file(s).";
  }

  @Override
  public double getFinishedPercentage() {
    // + 1 because we count the generation of the data sets, too.
    return ((double) doneFiles / (rawDataFiles.size() + 1));
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // TODO: new ModularFeatureList name
    ModularFeatureList newFeatureList = new ModularFeatureList("Feature list " + this.hashCode(),
        null, new ArrayList<>(rawDataFiles));

    for (RawDataFile rawDataFile : rawDataFiles) {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }

      ManualFeature feature = ManualFeatureUtils.pickFeatureManually(rawDataFile,
          rawDataFile.getDataRTRange(scanSelection.getMsLevelFilter().getSingleMsLevelOrNull()),
          mzRange);
      if (feature != null && feature.getScanNumbers() != null
          && feature.getScanNumbers().length > 0) {
        feature.setFeatureList(newFeatureList);
        ModularFeature modularFeature = FeatureConvertors.ManualFeatureToModularFeature(
            newFeatureList, feature);
        features.add(new FeatureDataSet(modularFeature));
      } else {
        logger.finest("No scans found for " + rawDataFile.getName());
      }
      doneFiles++;
    }

    Platform.runLater(() -> {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }
      chromPlot.removeAllFeatureDataSets(false);
      chromPlot.addFeatureDataSets(features);
    });

    setStatus(TaskStatus.FINISHED);
  }
}
