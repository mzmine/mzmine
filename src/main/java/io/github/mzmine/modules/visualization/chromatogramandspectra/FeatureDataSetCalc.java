/*
 * Copyright 2006-2021 The MZmine Development Team
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
    return "Calculating base peak chromatogram(s) of m/z "
        + mzFormat.format((mzRange.upperEndpoint() + mzRange.lowerEndpoint()) / 2) + " in "
        + rawDataFiles.size() + " file(s).";
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
          rawDataFile.getDataRTRange(scanSelection.getMsLevel()), mzRange);
      if (feature != null && feature.getScanNumbers() != null
          && feature.getScanNumbers().length > 0) {
        feature.setFeatureList(newFeatureList);
        ModularFeature modularFeature =
            FeatureConvertors.ManualFeatureToModularFeature(newFeatureList, feature);
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
