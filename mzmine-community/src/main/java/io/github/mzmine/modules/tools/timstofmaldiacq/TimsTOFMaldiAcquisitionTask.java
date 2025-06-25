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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.PrecursorSelectionModule;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TimsTOFPrecursorSelectionOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimsTOFMaldiAcquisitionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      TimsTOFMaldiAcquisitionTask.class.getName());

  public final FeatureList[] flists;
  public final ParameterSet parameters;
  private final Double maxMobilityWidth;
  private final Double minMobilityWidth;
  private final @NotNull PrecursorSelectionModule precursorSelectionModule;
  private final File acqControl;
  private final Integer initialOffsetY;
  private final Integer incrementOffsetX;
  private final File savePathDir;
  private final Boolean exportOnly;
  private final Boolean enableCeStepping;
  private final CeSteppingTables ceSteppingTables;
  private final Double isolationWidth;
  private final List<Double> collisionEnergies;
  private final int maxXIncrement;
  private String desc = "Running MAlDI acquisition";
  private double progress = 0d;
  private File currentCeFile = null;

  protected TimsTOFMaldiAcquisitionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, ParameterSet parameters, @NotNull MZmineProject project) {
    super(storage, moduleCallDate);
    this.parameters = parameters;

    flists = parameters.getValue(TimsTOFMaldiAcquisitionParameters.flists)
        .getMatchingFeatureLists();
    maxMobilityWidth = parameters.getValue(TimsTOFMaldiAcquisitionParameters.maxMobilityWidth);
    minMobilityWidth = parameters.getValue(TimsTOFMaldiAcquisitionParameters.minMobilityWidth);
    acqControl = parameters.getValue(TimsTOFMaldiAcquisitionParameters.acquisitionControl);
    initialOffsetY = parameters.getValue(TimsTOFMaldiAcquisitionParameters.initialOffsetY);
    incrementOffsetX = parameters.getValue(TimsTOFMaldiAcquisitionParameters.incrementOffsetX);
    savePathDir = parameters.getValue(TimsTOFMaldiAcquisitionParameters.savePathDir);
    exportOnly = parameters.getValue(TimsTOFMaldiAcquisitionParameters.exportOnly);
    isolationWidth = parameters.getValue(TimsTOFMaldiAcquisitionParameters.isolationWidth);
    var precursorSelParam = parameters.getParameter(
        TimsTOFMaldiAcquisitionParameters.precursorSelectionModule).getValueWithParameters();
    precursorSelectionModule = TimsTOFPrecursorSelectionOptions.createSelector(precursorSelParam);
    maxXIncrement = parameters.getValue(TimsTOFMaldiAcquisitionParameters.maxIncrementSteps);
    enableCeStepping = parameters.getValue(TimsTOFMaldiAcquisitionParameters.ceStepping);
    if (enableCeStepping) {
      collisionEnergies = parameters.getParameter(TimsTOFMaldiAcquisitionParameters.ceStepping)
          .getEmbeddedParameter().getValue();
      ceSteppingTables = new CeSteppingTables(collisionEnergies, isolationWidth);
    } else {
      ceSteppingTables = null;
      collisionEnergies = null;
    }
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!savePathDir.exists()) {
      savePathDir.mkdirs();
    }

    // todo: - ce ramp
    //       - export only

    // we can (in the future) acquire multiple spots in one file, but not multiple CEs in one file

    final int numCes = ceSteppingTables != null ? ceSteppingTables.getNumberOfCEs() : 1;

    final File acqFile = new File(savePathDir, "acquisition.txt");
    acqFile.delete();

    for (int flistCounter = 0; flistCounter < flists.length; flistCounter++) {
      final double flistStepProgress = 1 / (double) flists.length;

      if (isCanceled()) {
        return;
      }

      final FeatureList flist = flists[flistCounter];
      progress = flistCounter / (double) Math.max((flists.length - 1), 1);

      if (flist.getNumberOfRows() == 0) {
        continue;
      }
      final IMSRawDataFile file = (IMSRawDataFile) flist.getRawDataFile(0);
      final Range<Float> dataMobilityRange = RangeUtils.toFloatRange(file.getDataMobilityRange());

      final double mobilityGap = TimsTOFAcquisitionUtils.getOneOverK0DistanceForSwitchTime(
          file.getFrame(0), 1.65d);
      logger.finest("Mobility gap for file %s determined as 1/K0 = %.3f".formatted(file.getName(),
          mobilityGap));

      final List<MaldiTimsPrecursor> precursors = flist.getRows().stream().filter(
          row -> row.getBestFeature() != null
                 && row.getBestFeature().getFeatureStatus() != FeatureStatus.UNKNOWN
                 && row.getBestFeature().getMobility() != null
                 && row.getBestFeature().getMobilityRange() != null).map(row -> {
        final Feature f = row.getBestFeature();
        Range<Float> mobilityRange = TimsTOFAcquisitionUtils.adjustMobilityRange(f.getMobility(),
            f.getMobilityRange(), minMobilityWidth, maxMobilityWidth, dataMobilityRange);

        return new MaldiTimsPrecursor(f, f.getMZ(), mobilityRange, collisionEnergies);
      }).toList();

      final List<String> spotNames = precursors.stream().map(precursor -> {
        final Scan scan = precursor.feature().getRepresentativeScan();
        if (!(scan instanceof ImagingFrame imgFrame)) {
          throw new IllegalStateException(
              "Representative scan of feature " + precursor.toString() + " is not an ImagingFrame");
        }
        final MaldiSpotInfo maldiSpotInfo = imgFrame.getMaldiSpotInfo();
        if (maldiSpotInfo == null) {
          throw new IllegalStateException(
              "Maldi spot info for frame " + imgFrame.toString() + " is null.");
        }
        return maldiSpotInfo.spotName();
      }).distinct().toList();

      if (spotNames.size() != 1) {
        throw new IllegalStateException(
            "No or more than one spot in feature list " + flist.getName());
      }

      var spotName = spotNames.get(0);

      final List<List<MaldiTimsPrecursor>> precursorLists = precursorSelectionModule.getPrecursorList(
          precursors, mobilityGap);

      int spotIncrement = 1;
      for (int ceCounter = 0; ceCounter < numCes; ceCounter++) {
        final double ceStepProgress = 1 / (double) numCes;

        if (enableCeStepping) {
          assert ceSteppingTables != null;
          currentCeFile = new File(savePathDir,
              "ce_table_" + ceSteppingTables.getCE(ceCounter) + "eV.csv");
          final boolean success = ceSteppingTables.writeCETable(ceCounter, currentCeFile);
          if (!success) {
            setErrorMessage("Cannot write CE table.");
            setStatus(TaskStatus.ERROR);
            return;
          }
        }

        for (int i = 0; i < precursorLists.size(); i++) {
          if (isCanceled()) {
            return;
          }

          final double precursorListProgress = i / (double) precursorLists.size();
          progress = ceCounter * ceStepProgress + (ceStepProgress * flistStepProgress
                                                   * precursorListProgress);

          List<MaldiTimsPrecursor> precursorList = precursorLists.get(i);
          final String fileName =
              spotName + "_msms_" + (i + 1) + (enableCeStepping ? "_ce_" + ceSteppingTables.getCE(
                  ceCounter) + "eV" : "");
          desc = "Acquiring " + fileName;

          final int[] offsets = TimsTOFAcquisitionUtils.getOffsetsForIncrementCounter(spotIncrement,
              maxXIncrement, incrementOffsetX, initialOffsetY);
          try {
            TimsTOFAcquisitionUtils.appendToCommandFile(acqFile, spotName, precursorList,
                offsets[0], offsets[1], null, null, (i + 1), savePathDir, fileName, currentCeFile,
                enableCeStepping, null);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          spotIncrement++;
        }
      }
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(TimsTOFMaldiAcquisitionModule.class, parameters,
              getModuleCallDate()));
    }

    TimsTOFAcquisitionUtils.acquire(acqControl, acqFile, exportOnly);
    setStatus(TaskStatus.FINISHED);
  }
}
