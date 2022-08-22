/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.PrecursorSelectionModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
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
  private final @Nullable ParameterSet precursorSelectionParameters;
  private final File acqControl;
  private final Integer initialOffsetY;
  private final Integer incrementOffsetX;
  private final File savePathDir;
  private final Boolean exportOnly;
  private final Boolean enableCeStepping;
  private final CeSteppingTables ceSteppingTables;
  private final Double isolationWidth;

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
    precursorSelectionModule = parameters.getValue(
        TimsTOFMaldiAcquisitionParameters.precursorSelectionModule).getModule();
    precursorSelectionParameters = parameters.getValue(
        TimsTOFMaldiAcquisitionParameters.precursorSelectionModule).getParameterSet();
    enableCeStepping = parameters.getValue(TimsTOFMaldiAcquisitionParameters.ceStepping);
    if (enableCeStepping) {
      ceSteppingTables = new CeSteppingTables(
          parameters.getParameter(TimsTOFMaldiAcquisitionParameters.ceStepping)
              .getEmbeddedParameter().getValue(), isolationWidth);
    } else {
      ceSteppingTables = null;
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

      final List<MaldiTimsPrecursor> precursors = flist.getRows().stream().filter(
          row -> row.getBestFeature() != null
              && row.getBestFeature().getFeatureStatus() != FeatureStatus.UNKNOWN
              && row.getBestFeature().getMobility() != null
              && row.getBestFeature().getMobilityRange() != null).map(row -> {
        final Feature f = row.getBestFeature();
        Range<Float> mobilityRange = TimsTOFAcquisitionUtils.adjustMobilityRange(f.getMobility(),
            f.getMobilityRange(), minMobilityWidth, maxMobilityWidth);

        return new MaldiTimsPrecursor(f, f.getMZ(), mobilityRange, null);
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
          precursors, precursorSelectionParameters);

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

          try {
            TimsTOFAcquisitionUtils.appendToCommandFile(acqFile, spotName, precursorList,
                initialOffsetY, incrementOffsetX, null, null, (i + 1), spotIncrement, savePathDir,
                fileName, currentCeFile, enableCeStepping);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          spotIncrement++;
        }
      }

      TimsTOFAcquisitionUtils.acquire(acqControl, acqFile, exportOnly);
    }

    setStatus(TaskStatus.FINISHED);
  }

}
