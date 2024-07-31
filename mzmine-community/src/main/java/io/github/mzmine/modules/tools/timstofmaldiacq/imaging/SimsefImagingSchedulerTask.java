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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.mainwindow.FeatureListSummaryController;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.CeSteppingTables;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFMaldiAcquisitionTask;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters.MaldiMs2AcquisitionWriter;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters.MaldiMs2AcquisitionWriters;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters.SingleSpotMs2Writer;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeDouble;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimsefImagingSchedulerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      TimsTOFMaldiAcquisitionTask.class.getName());
  public final FeatureList[] flists;
  public final ParameterSet parameters;
  private final boolean preview;
  private final Double maxMobilityWidth;
  private final Double minMobilityWidth;
  private final File acqControl;
  private final File savePathDir;
  private final Boolean exportOnly;
  private final Double isolationWidth;
  private final MZTolerance isolationWindow;
  private final int numMsMs;
  private final AbsoluteAndRelativeDouble minMsMsIntensity;
  private final int minDistance;

  private final double minPurityScore;
  private final boolean scheduleOnly;
  private final int[] spotsFeatureCounter;

  private final Ms2ImagingMode ms2ImagingMode;
  private final @NotNull MaldiMs2AcquisitionWriter ms2Module;
  private final List<Double> collisionEnergies;
  private final CeSteppingTables ceSteppingTables;
  private final int totalMsMsPerFeature;
  private final Map<Feature, List<ImagingSpot>> featureSpotMap = new HashMap<>();
  private final Map<ImagingFrame, ImagingSpot> frameSpotMap = new HashMap<>();
  private final AdvancedParametersParameter<AdvancedImageMsMsParameters> advancedParam;
  private String desc = "Scheduling precursors MALDI acquisition";
  private double progress = 0d;
  private File currentCeFile = null;

  protected SimsefImagingSchedulerTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, ParameterSet parameters, @NotNull MZmineProject project,
      boolean scheduleOnly, boolean preview) {
    super(storage, moduleCallDate);
    this.parameters = parameters.cloneParameterSet();
    this.preview = preview;

    advancedParam = parameters.getParameter(SimsefImagingSchedulerParameters.advancedParameters);

    flists = parameters.getValue(SimsefImagingSchedulerParameters.flists).getMatchingFeatureLists();
    maxMobilityWidth = advancedParam.getValueOrDefault(AdvancedImageMsMsParameters.maxMobilityWidth,
        AdvancedImageMsMsParameters.MAX_MOBILITY_WIDTH);
    minMobilityWidth = advancedParam.getValueOrDefault(AdvancedImageMsMsParameters.minMobilityWidth,
        AdvancedImageMsMsParameters.MIN_MOBILITY_WIDTH);
    acqControl = parameters.getValue(SimsefImagingSchedulerParameters.acquisitionControl);
    savePathDir = parameters.getValue(SimsefImagingSchedulerParameters.savePathDir);
    exportOnly = parameters.getValue(SimsefImagingSchedulerParameters.exportOnly);
    isolationWidth = advancedParam.getValueOrDefault(AdvancedImageMsMsParameters.isolationWidth,
        AdvancedImageMsMsParameters.MIN_ISOLATION_WIDTH);
    numMsMs = parameters.getValue(SimsefImagingSchedulerParameters.numMsMs);
    collisionEnergies = parameters.getValue(SimsefImagingSchedulerParameters.collisionEnergies);
    minMsMsIntensity = parameters.getValue(SimsefImagingSchedulerParameters.minimumIntensity);
    minDistance = parameters.getValue(SimsefImagingSchedulerParameters.minimumDistance);
    minPurityScore = parameters.getValue(SimsefImagingSchedulerParameters.minimumPurity);
    this.scheduleOnly = scheduleOnly;
    isolationWindow = new MZTolerance((isolationWidth * 1.3) / 2,
        0d); // isolation window typically wider than set
//    ms2Module = parameters.getValue(TimsTOFImageMsMsParameters.ms2ImagingMode).getModule();
    if (advancedParam.getValueOrDefault(AdvancedImageMsMsParameters.ms2ImagingMode, false)) {
      var acquisitionModeParam = advancedParam.getEmbeddedParameters()
          .getParameter(AdvancedImageMsMsParameters.ms2ImagingMode).getEmbeddedParameter()
          .getValueWithParameters();
      ms2Module = MaldiMs2AcquisitionWriters.createOption(acquisitionModeParam);
    } else {
      ms2Module = MaldiMs2AcquisitionWriters.createDefault();
    }
    ms2ImagingMode = ms2Module.equals(MZmineCore.getModuleInstance(SingleSpotMs2Writer.class))
        ? Ms2ImagingMode.SINGLE : Ms2ImagingMode.TRIPLE;

    totalMsMsPerFeature = numMsMs * collisionEnergies.size();
    spotsFeatureCounter = new int[totalMsMsPerFeature + 1];

    ceSteppingTables = new CeSteppingTables(collisionEnergies, isolationWidth);
  }

  public Map<Feature, List<ImagingSpot>> getFeatureSpotMap() {
    return featureSpotMap;
  }

  public Map<ImagingFrame, ImagingSpot> getFrameSpotMap() {
    return frameSpotMap;
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

    if (isCanceled()) {
      return;
    }

    final FeatureList flist = flists[0];
    final IMSImagingRawDataFile file = (IMSImagingRawDataFile) flist.getRawDataFile(0);

    final MobilityScanDataAccess access = new MobilityScanDataAccess(file,
        MobilityScanDataType.MASS_LIST, (List<Frame>) file.getFrames(1));

    if (flist.getNumberOfRows() == 0) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    List<FeatureListRow> rows = new ArrayList<>(flist.getRows());
    // sort low to high area. First find spots for low intensity features so we definitely fragment
    // those. should be easier to find spots for high area features
    rows.sort(Comparator.comparingDouble(FeatureListRow::getMaxArea));

    final double minMobilityDistance = getQuadSwitchTime(flist);
    for (int i = 0; i < rows.size(); i++) {
      progress = i / (double) rows.size();

      final FeatureListRow row = rows.get(i);
      if (isCanceled()) {
        return;
      }

      final Feature f = row.getBestFeature();
      if (f.getHeight() < minMsMsIntensity.getMaximumValue(f.getHeight())) {
        continue;
      }

      final Range<Float> mobilityBounds = RangeUtils.toFloatRange(file.getDataMobilityRange());
      final MaldiTimsPrecursor precursor = new MaldiTimsPrecursor(f, f.getMZ(),
          TimsTOFAcquisitionUtils.adjustMobilityRange(f.getMobility(), f.getMobilityRange(),
              minMobilityWidth, maxMobilityWidth, mobilityBounds), collisionEnergies);

      final IonTimeSeries<? extends Scan> data = f.getFeatureData();
      final IonTimeSeries<? extends ImagingFrame> imagingData = (IonTimeSeries<? extends ImagingFrame>) data;

      final var minFeatureIntensity = minMsMsIntensity.getMaximumValue(
          f.getHeight());//Math.max(minMsMsIntensity, f.getHeight() * 0.01);

      // check existing msms spots first
      addEntriesToExistingSpots(access, minFeatureIntensity, frameSpotMap, precursor, imagingData,
          numMsMs, featureSpotMap, minDistance, minPurityScore, minMobilityDistance);

      // we have all needed entries
      if (precursor.getLowestMsMsCountForCollisionEnergies() >= numMsMs) {
        continue;
      }

      if (featureSpotMap.size() == access.getNumberOfScans()) {
        logger.warning(() -> "Too many MS/MS spots, cannot create any more.");
        // can still add more features to existing spots
        continue;
      }

      // find new entries
      createNewMsMsSpots(access, frameSpotMap, minFeatureIntensity, imagingData, precursor, numMsMs,
          featureSpotMap, minDistance, minPurityScore, minMobilityDistance);

      spotsFeatureCounter[precursor.getTotalMsMs()]++;
    }

    for (int i = 0; i < spotsFeatureCounter.length; i++) {
      final int j = i;
      logger.finest(
          () -> String.format("%d features have %d MS/MS spots. (%.1f)", (spotsFeatureCounter[j]),
              j, (spotsFeatureCounter[j] / (double) rows.size() * 100)));
    }

    if (preview) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(SimsefImagingSchedulerModule.class, parameters,
            getModuleCallDate()));
    if (scheduleOnly) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final File acqFile = new File(savePathDir, "acquisition.txt");
    acqFile.delete();

    // sort the spots by line, so we limit the movement that we have to do
    final List<ImagingSpot> sortedSpots = frameSpotMap.entrySet().stream().sorted((e1, e2) -> {
      int xCompare = Integer.compare(e1.getKey().getMaldiSpotInfo().xIndexPos(),
          e2.getValue().spotInfo().xIndexPos());
      if (xCompare != 0) {
        return xCompare;
      }
      return Integer.compare(e1.getKey().getMaldiSpotInfo().yIndexPos(),
          e2.getKey().getMaldiSpotInfo().yIndexPos());
    }).map(Entry::getValue).toList();

    ms2Module.writeAcqusitionFile(acqFile, sortedSpots, ceSteppingTables, this::isCanceled,
        savePathDir);

    try {
      dumpImagingSpotInfos(sortedSpots);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (!isCanceled()) {
      desc = "Running MALDI Acquisition.";
      TimsTOFAcquisitionUtils.acquire(acqControl, acqFile, exportOnly);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void createNewMsMsSpots(MobilityScanDataAccess access,
      Map<ImagingFrame, ImagingSpot> spotMap, double minMsMsIntensity,
      IonTimeSeries<? extends ImagingFrame> imagingData, MaldiTimsPrecursor precursor, int numMsMs,
      Map<Feature, List<ImagingSpot>> featureSpotMap, double minDistance, double minPurityScore,
      final double minMobilityDistance) {

    final int[] intensitySortedIndices = IonTimeSeriesUtils.getIntensitySortedIndices(imagingData);
    final List<ImagingSpot> spots = featureSpotMap.computeIfAbsent(precursor.feature(),
        f -> new ArrayList<>());

    for (int i = 0;
        i < intensitySortedIndices.length && precursor.getTotalMsMs() < totalMsMsPerFeature; i++) {
      final int nextIndex = intensitySortedIndices[i];
      final ImagingFrame frame = imagingData.getSpectrum(nextIndex);

      if (spotMap.containsKey(frame)) {
        // spot already used with a different collision energy and this feature did not fit
        continue;
      }

      final MaldiSpotInfo spotInfo = frame.getMaldiSpotInfo();
      if (spotInfo == null) {
        continue;
      }

      // sorted by intensity, if we are below the threshold, don't add a new scan
      if (imagingData.getIntensity(nextIndex) < minMsMsIntensity) {
        break;
      }

      // check if we can make this spot a new one with a new collision energy
      final Double collisionEnergy = TimsTOFAcquisitionUtils.getBestCollisionEnergyForSpot(
          minDistance, precursor, spots, spotInfo, collisionEnergies, 3);

      if (collisionEnergy == null) {
        continue;
      }

      if (!checkPurityScore(access, precursor, minPurityScore, frame)) {
        continue;
      }

      final ImagingSpot spot = spotMap.computeIfAbsent(frame,
          a -> new ImagingSpot(a.getMaldiSpotInfo(), ms2ImagingMode, collisionEnergy));
      if (spot.addPrecursor(precursor, minMobilityDistance)) {
        spots.add(spot);
      }
    }
  }

  private void addEntriesToExistingSpots(MobilityScanDataAccess access, double minMsMsIntensity,
      Map<ImagingFrame, ImagingSpot> spotMap, MaldiTimsPrecursor precursor,
      IonTimeSeries<? extends ImagingFrame> imagingData, final int numMsMs,
      Map<Feature, List<ImagingSpot>> featureSpotMap, double minDistance,
      final double minChimerityScore, final double minMobilityDistance) {
    final List<ImagingFrame> usedFrames = getPossibleExistingSpots(spotMap, imagingData,
        minMsMsIntensity);

    final List<ImagingSpot> spots = featureSpotMap.computeIfAbsent(precursor.feature(),
        f -> new ArrayList<>());
    for (ImagingFrame usedFrame : usedFrames) {
      final ImagingSpot imagingSpot = spotMap.get(usedFrame);

      if (!imagingSpot.checkPrecursor(precursor, minMobilityDistance)) {
        continue;
      }

      // check if we meet the minimum distance requirement
      final List<Double> collisionEnergy = TimsTOFAcquisitionUtils.getPossibleCollisionEnergiesForSpot(
          minDistance, precursor, spots, usedFrame.getMaldiSpotInfo(), collisionEnergies, numMsMs);
      if (collisionEnergy.isEmpty() || !collisionEnergy.contains(
          imagingSpot.getCollisionEnergy())) {
        continue;
      }

      if (!checkPurityScore(access, precursor, minChimerityScore, usedFrame)) {
        continue;
      }

      // check if the entry fits into the precursor ramp at that spot
      if (imagingSpot.addPrecursor(precursor, minMobilityDistance)) {
        spots.add(imagingSpot);
      }

      if (precursor.getLowestMsMsCountForCollisionEnergies() >= numMsMs) {
        break;
      }
    }
  }

  private boolean checkPurityScore(MobilityScanDataAccess access, MaldiTimsPrecursor precursor,
      double minPurityScore, ImagingFrame usedFrame) {
    access.jumpToFrame(usedFrame);

    final double purityScore = IonMobilityUtils.getPurityInMzAndMobilityRange(precursor.mz(),
        access, isolationWindow.getToleranceRange(precursor.mz()), precursor.mobility(), true);
    if (purityScore < minPurityScore) {
      return false;
    }
    return true;
  }

  /**
   * @param spotMap          The current spots selected for MS/MS experiments.
   * @param imagingData      The {@link Feature#getFeatureData()} of the image feature.
   * @param minMsMsIntensity The minimum intensity for an MS/MS.
   * @return A list of all spots currently selected for MS/MS experiments, that the feature has the
   * minimum intensity in.
   */
  private List<ImagingFrame> getPossibleExistingSpots(Map<ImagingFrame, ImagingSpot> spotMap,
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

  private void dumpImagingSpotInfos(List<ImagingSpot> spots) throws IOException {
    desc = "Dumping msms info";
    progress = 0;
    double step = 1d / spots.size() * 0.5;

    final StringBuilder spotBuilder = new StringBuilder();
    spotBuilder.append("spot_name,x_index,y_index,ce,num_precursors,precursor_ids\n");
    for (ImagingSpot spot : spots) {
      final MaldiSpotInfo info = spot.spotInfo();
      spotBuilder.append(info.spotName()).append(",");
      spotBuilder.append(info.xIndexPos()).append(",");
      spotBuilder.append(info.yIndexPos()).append(",");
      spotBuilder.append(spot.getCollisionEnergy()).append(",");
      final List<MaldiTimsPrecursor> precursorList = spot.getPrecursorList(0, 0);
      spotBuilder.append(precursorList.size()).append(",");
      spotBuilder.append("{");
      spotBuilder.append(precursorList.stream().map(p -> p.feature().getRow().getID().toString())
          .collect(Collectors.joining(";")));
      spotBuilder.append("}");
      spotBuilder.append("\n");
      progress += step;
    }

    final List<MaldiTimsPrecursor> precursors = spots.stream()
        .flatMap(spot -> spot.getPrecursorList(0, 0).stream()).distinct()
        .sorted(Comparator.comparingDouble(p -> p.feature().getHeight())).toList();

    step = 1d / precursors.size();
    final StringBuilder precursorBuilder = new StringBuilder();
    precursorBuilder.append("id,height,area,mz,spots_above_threshold,total_spots,");
    for (Double ce : collisionEnergies) {
      precursorBuilder.append("spots_").append(ce).append(",");
    }
    precursorBuilder.append("\n");
    for (MaldiTimsPrecursor precursor : precursors) {
      precursorBuilder.append(precursor.feature().getRow().getID()).append(",");
      precursorBuilder.append(precursor.feature().getHeight()).append(",");
      precursorBuilder.append(precursor.feature().getArea()).append(",");
      precursorBuilder.append(precursor.mz()).append(",");
      precursorBuilder.append(getSpotsAboveThreshold(precursor)).append(",");
      precursorBuilder.append(precursor.getTotalMsMs()).append(",");
      for (Double ce : collisionEnergies) {
        precursorBuilder.append(precursor.getMsMsSpotsForCollisionEnergy(ce)).append(",");
      }
      precursorBuilder.append("\n");
      progress += step;
    }

    final File spotFile = new File(savePathDir, "spots.csv");
    spotFile.createNewFile();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(spotFile))) {
      writer.write(spotBuilder.toString());
    }

    final File precursorFile = new File(savePathDir, "precursors.csv");
    precursorFile.createNewFile();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(precursorFile))) {
      writer.write(precursorBuilder.toString());
    }

    final File parametersFile = new File(savePathDir, "parameters.txt");
    parametersFile.createNewFile();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(parametersFile))) {
      for (Parameter<?> parameter : parameters.getParameters()) {
        writer.write(FeatureListSummaryController.parameterToString(parameter));
        writer.newLine();
      }
    }
  }

  private int getSpotsAboveThreshold(MaldiTimsPrecursor p) {
    final IonTimeSeries<? extends Scan> data = p.feature().getFeatureData();

    int counter = 0;
    for (int i = 0; i < data.getNumberOfValues(); i++) {
      if (data.getIntensity(i) > minMsMsIntensity.getMaximumValue(p.feature().getHeight())) {
        counter++;
      }
    }

    return counter;
  }

  private double getQuadSwitchTime(FeatureList flist) {
    final Double switchTime = advancedParam.getValueOrDefault(
        AdvancedImageMsMsParameters.quadSwitchTime, AdvancedImageMsMsParameters.QUAD_SWITCH_TIME);

    final Scan representativeScan = flist.getRows().get(0).getBestFeature().getRepresentativeScan();
    if (representativeScan instanceof Frame frame) {
      return TimsTOFAcquisitionUtils.getOneOverK0DistanceForSwitchTime(frame, switchTime);
    }
    throw new IllegalArgumentException("Not an IMS file.");
  }
}
