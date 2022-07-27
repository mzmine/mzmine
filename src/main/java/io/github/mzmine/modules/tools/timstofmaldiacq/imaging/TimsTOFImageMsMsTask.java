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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import com.google.common.collect.Range;
import com.google.common.io.Files;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.CeSteppingTables;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFMaldiAcquisitionTask;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimsTOFImageMsMsTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      TimsTOFMaldiAcquisitionTask.class.getName());

  public final FeatureList[] flists;
  public final ParameterSet parameters;
  private final Double maxMobilityWidth;
  private final Double minMobilityWidth;
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

  protected TimsTOFImageMsMsTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, ParameterSet parameters, @NotNull MZmineProject project) {
    super(storage, moduleCallDate);
    this.parameters = parameters;

    flists = parameters.getValue(TimsTOFImageMsMsParameters.flists).getMatchingFeatureLists();
    maxMobilityWidth = parameters.getValue(TimsTOFImageMsMsParameters.maxMobilityWidth);
    minMobilityWidth = parameters.getValue(TimsTOFImageMsMsParameters.minMobilityWidth);
    acqControl = parameters.getValue(TimsTOFImageMsMsParameters.acquisitionControl);
    initialOffsetY = parameters.getValue(TimsTOFImageMsMsParameters.initialOffsetY);
    incrementOffsetX = parameters.getValue(TimsTOFImageMsMsParameters.incrementOffsetX);
    savePathDir = parameters.getValue(TimsTOFImageMsMsParameters.savePathDir);
    exportOnly = parameters.getValue(TimsTOFImageMsMsParameters.exportOnly);
    isolationWidth = parameters.getValue(TimsTOFImageMsMsParameters.isolationWidth);
    enableCeStepping = parameters.getValue(TimsTOFImageMsMsParameters.ceStepping);
    if (enableCeStepping) {
      ceSteppingTables = new CeSteppingTables(
          parameters.getParameter(TimsTOFImageMsMsParameters.ceStepping).getEmbeddedParameter()
              .getValue(), isolationWidth);
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

    final double flistStepProgress = 1 / (double) flists.length;

    if (isCanceled()) {
      return;
    }

    final FeatureList flist = flists[0];

    if (flist.getNumberOfRows() == 0) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final int numMsMs = 3;
    final double minDistance = 30;
    final double minMsMsIntensity = 10_000;

    final Map<ImagingFrame, ImagingSpot> spotMap = new HashMap<>();
    ObservableList<FeatureListRow> rows = flist.getRows();
    for (int i = 0; i < rows.size(); i++) {
      progress = 0.1 * i / (double) rows.size();

      final FeatureListRow row = rows.get(i);
      if (isCanceled()) {
        return;
      }

      final Feature f = row.getBestFeature();
      if (f.getHeight() < minMsMsIntensity) {
        continue;
      }

      final MaldiTimsPrecursor precursor = new MaldiTimsPrecursor(f, f.getMZ(),
          TimsTOFAcquisitionUtils.adjustMobilityRange(f.getMobility(), f.getMobilityRange(),
              minMobilityWidth, maxMobilityWidth), 0f);

      final IonTimeSeries<? extends Scan> data = f.getFeatureData();
      final IonTimeSeries<? extends ImagingFrame> imagingData = (IonTimeSeries<? extends ImagingFrame>) data;

      // check existing msms spots first
      int createdMsMsEntries = addEntriesToExistingSpots(minMsMsIntensity, spotMap, precursor,
          imagingData, numMsMs);

      // we have all needed entries
      if (createdMsMsEntries >= numMsMs) {
        continue;
      }

      // find new entries
      createdMsMsEntries = createNewMsMsSpots(spotMap, minMsMsIntensity, imagingData, precursor,
          numMsMs, createdMsMsEntries);

      logger.finest(() -> "Did not find enough MSMS spots for feature " + f.toString());
    }

    final List<ImagingSpot> sortedSpots = spotMap.entrySet().stream().sorted((e1, e2) -> {
      int xCompare = Integer.compare(e1.getKey().getMaldiSpotInfo().xIndexPos(),
          e2.getValue().spotInfo().xIndexPos());
      if (xCompare != 0) {
        return xCompare;
      }
      return Integer.compare(e1.getKey().getMaldiSpotInfo().yIndexPos(),
          e2.getKey().getMaldiSpotInfo().yIndexPos());
    }).map(Entry::getValue).toList();

    for (int i = 0; i < sortedSpots.size(); i++) {
      final ImagingSpot spot = sortedSpots.get(i);

      progress = 0.2 + 0.8 * i / sortedSpots.size();
      if (isCanceled()) {
        return;
      }

      final MaldiSpotInfo spotInfo = spot.spotInfo();

      int counter = 1;
      for (int x = 0; x < 2; x++) {
        for (int y = 0; y < 2; y++) {
          if (x == 0 && y == 0 || spot.getPrecursorList(x, y).isEmpty()) {
            continue;
          }
          TimsTOFAcquisitionUtils.acquireAbsOffset(acqControl, spotInfo.spotName(),
              spot.getPrecursorList(x, y), x * initialOffsetY, y * incrementOffsetX, counter++,
              savePathDir, spotInfo.spotName() + "_" + counter, null, false, exportOnly);
        }
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private int createNewMsMsSpots(Map<ImagingFrame, ImagingSpot> spotMap, double minMsMsIntensity,
      IonTimeSeries<? extends ImagingFrame> imagingData, MaldiTimsPrecursor precursor, int numMsMs,
      int currentNumSpots) {
    final IntensitySortedSeries<IonTimeSeries<? extends ImagingFrame>> imagingSorted = new IntensitySortedSeries<>(
        imagingData);

    while (imagingSorted.hasNext() && currentNumSpots < numMsMs) {
      final Integer nextIndex = imagingSorted.next();
      final ImagingFrame frame = imagingData.getSpectrum(nextIndex);

      // sorted by intensity, if we are below the threshold, don't add a new scan
      if (imagingData.getIntensity(nextIndex) < minMsMsIntensity) {
        break;
      }

      final MaldiSpotInfo spotInfo = frame.getMaldiSpotInfo();
      if (spotInfo == null) {
        continue;
      }

      final ImagingSpot spot = spotMap.computeIfAbsent(frame,
          a -> new ImagingSpot(a.getMaldiSpotInfo()));
      if (spot.addPrecursor(precursor)) {
        currentNumSpots++;
      }
    }
    return currentNumSpots;
  }

  private int addEntriesToExistingSpots(double minMsMsIntensity,
      Map<ImagingFrame, ImagingSpot> spotMap, MaldiTimsPrecursor precursor,
      IonTimeSeries<? extends ImagingFrame> imagingData, final int numMsMs) {
    int createdMsMsEntries = 0;
    final List<ImagingFrame> usedFrames = checkExistingSpots(spotMap, imagingData,
        minMsMsIntensity);
    for (ImagingFrame usedFrame : usedFrames) {
      final ImagingSpot imagingSpot = spotMap.get(usedFrame);
      if (imagingSpot.addPrecursor(precursor)) {
        createdMsMsEntries++;
      }
      if (createdMsMsEntries >= numMsMs) {
        break;
      }
    }
    return createdMsMsEntries;
  }

  private List<ImagingFrame> checkExistingSpots(Map<ImagingFrame, ImagingSpot> spotMap,
      IonTimeSeries<? extends ImagingFrame> imagingData, double minMsMsIntensity) {
    final List<ImagingFrame> frames = new ArrayList<>();
    final List<? extends ImagingFrame> spectra = imagingData.getSpectra();

    for (int i = 0; i < spectra.size(); i++) {
      if (imagingData.getIntensity(i) < minMsMsIntensity) {
        continue;
      }
      final ImagingFrame frame = spectra.get(i);
      if (spotMap.containsKey(frame)) {
        frames.add(frame);
      }
    }
    return frames;
  }

  private boolean acquire(final File acqControl, final String spot,
      final List<MaldiTimsPrecursor> precursorList, final int initialOffsetY,
      final int incrementOffsetX, int precursorListCounter, int spotIncrement,
      final File savePathDir, String name, File currentCeFile) {
    List<String> cmdLine = new ArrayList<>();

    cmdLine.add(acqControl.toString());
    cmdLine.add("--spot");
    cmdLine.add(spot);

    cmdLine.add("--xoffset");
    cmdLine.add(String.valueOf(incrementOffsetX * spotIncrement));
    cmdLine.add("--yoffset");
    cmdLine.add(String.valueOf(initialOffsetY));

    cmdLine.add("--path");
    cmdLine.add(savePathDir.toString().replace(File.separatorChar, '/'));

    cmdLine.add("--name");
    cmdLine.add(name);

    cmdLine.add("--acqtype");
    cmdLine.add("accumulate");

    if (enableCeStepping && currentCeFile != null && currentCeFile.exists()) {
      cmdLine.add("--cetable");
      cmdLine.add(currentCeFile.toPath().toString());
    }

    replacePrecursorCsv(precursorList, true, spot, precursorListCounter);

    if (!exportOnly) {
      final ProcessBuilder builder = new ProcessBuilder(cmdLine).inheritIO();
      final Process process;

      try {
        process = builder.start();
        process.waitFor();
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING,
            "Could not acquire spot " + spot + ". Process finished irregularly.", e);

        return false;
      }
    }
    return true;
  }

  private void replacePrecursorCsv(List<MaldiTimsPrecursor> precursorList, boolean createCopy,
      String spot, int counter) {

    // if we only export, just create the export file
    final File csv =
        !exportOnly ? new File("C:\\BDALSystemData\\timsTOF\\maldi\\maldi_tims_precursors.csv")
            : new File(savePathDir, "maldi_tims_precursors_" + spot + "_msms_" + counter + ".csv");
    if (csv.exists()) {
      final boolean deleted = csv.delete();
      if (!deleted) {
        throw new IllegalStateException("Cannot delete maldi_tims_precursors.csv file.");
      }
    }

    try {
      csv.createNewFile();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot create maldi_tims_precursors.csv file", e);
      return;
    }

    try (var writer = new FileWriter(csv)) {

      BufferedWriter w = new BufferedWriter(writer);
      w.write("1");  // activate CE settings from the MALDI MS/MS tab
      w.newLine();

      // make sure the precursors are sorted
      precursorList.sort(Comparator.comparingDouble(p -> p.oneOverK0().lowerEndpoint()));

      for (final MaldiTimsPrecursor precursor : precursorList) {
        w.write(
            String.format("%.4f,%.3f,%.3f", precursor.mz(), precursor.oneOverK0().lowerEndpoint(),
                precursor.oneOverK0().upperEndpoint()));
        w.newLine();
      }

      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // only copy the file, if it was an actual acquisition
    if (createCopy && !exportOnly) {
      try {
        Files.copy(csv,
            new File(savePathDir, "maldi_tims_precursors_" + spot + "_msms_" + counter + ".csv"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Range<Float> adjustMobilityRange(Float mobility, Range<Float> initial,
      Double minMobilityWidth, Double maxMobilityWidth) {

    final Float initialLength = RangeUtils.rangeLength(initial);

    if (initialLength <= maxMobilityWidth && initialLength >= minMobilityWidth) {
      return initial;
    } else if (initialLength < minMobilityWidth) {
      return Range.closed((float) (mobility - minMobilityWidth / 2),
          (float) (mobility + minMobilityWidth / 2));
    } else if (initialLength > maxMobilityWidth) {
      return Range.closed((float) (mobility - maxMobilityWidth / 2),
          (float) (mobility + maxMobilityWidth / 2));
    }

    logger.fine(
        () -> String.format("Unexpected mobility range length: %.3f. Min = %.3f, Max = %.3f",
            initialLength, minMobilityWidth, maxMobilityWidth));
    return initial;
  }


}
