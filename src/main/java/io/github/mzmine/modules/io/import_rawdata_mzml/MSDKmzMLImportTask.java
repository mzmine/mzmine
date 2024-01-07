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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.math.Quantiles;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.MsdkScanWrapper;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.MsDataImportAndMassDetectWrapperTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLRawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.DateTimeUtils;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class reads mzML 1.0 and 1.1.0 files (<a
 * href="http://www.psidev.info/index.php?q=node/257">http://www.psidev.info/index.php?q=node/257</a>)
 * using the jmzml library (<a
 * href="http://code.google.com/p/jmzml/">http://code.google.com/p/jmzml/</a>).
 */
@SuppressWarnings("UnstableApiUsage")
public class MSDKmzMLImportTask extends AbstractTask {

  public static final Pattern watersPattern = Pattern.compile(
      "function=([1-9]+) process=[\\d]+ scan=[\\d]+");
  private static final Logger logger = Logger.getLogger(MSDKmzMLImportTask.class.getName());
  private final File file;
  private final InputStream fis;
  // advanced processing will apply mass detection directly to the scans
  private final boolean applyMassDetection;
  private final MZmineProject project;
  private final ParameterSet advancedParam;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private MzMLFileImportMethod msdkTask = null;
  private int totalScans = 0, parsedScans;
  private String description;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;
  private boolean denormalizeMSnScans;

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    this(project, fileToOpen, null, null, module, parameters, moduleCallDate, storage);
  }

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, InputStream fisToOpen,
      ParameterSet advancedParam, @NotNull final Class<? extends MZmineModule> module,
      @NotNull final ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.file = fileToOpen;
    this.fis = fisToOpen;
    this.project = project;
    description = "Importing raw data file: " + fileToOpen.getName();
    this.advancedParam = advancedParam;
    this.parameters = parameters;
    this.module = module;

    if (advancedParam != null) {
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
        this.ms1Detector = advancedParam.getParameter(
            AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter().getValue();
      }
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
        this.ms2Detector = advancedParam.getParameter(
            AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue();
      }
      denormalizeMSnScans = advancedParam.getValue(
          AdvancedSpectraImportParameters.denormalizeMSnScans);
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
  }


  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    RawDataFileImpl newMZmineFile;
    try {

      if (fis != null) {
        msdkTask = new MzMLFileImportMethod(fis);
      } else {
        msdkTask = new MzMLFileImportMethod(file);
      }
      addTaskStatusListener((task, newStatus, oldStatus) -> {
        if (newStatus == TaskStatus.CANCELED) {
          msdkTask.cancel();
        }
      });
      MzMLRawDataFile msdkTaskRes = msdkTask.execute();
      io.github.msdk.datamodel.RawDataFile msdkFile = msdkTask.getResult();

      if (isCanceled()) {
        return;
      }

      if (msdkFile == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("MSDK returned null");
        return;
      }
      totalScans = msdkFile.getScans().size();

      final boolean isIms = msdkFile.getScans().stream()
          .anyMatch(s -> s instanceof MzMLMsScan scan && scan.getMobility() != null);

      if (isIms) {
        newMZmineFile = new IMSRawDataFileImpl(this.file.getName(), file.getAbsolutePath(),
            storage);
      } else {
        newMZmineFile = new RawDataFileImpl(this.file.getName(), file.getAbsolutePath(), storage);
      }
      try {
        // set time
        if (msdkTaskRes.getStartTimeStamp() != null) {
          newMZmineFile.setStartTimeStamp(DateTimeUtils.parse(msdkTaskRes.getStartTimeStamp()));
        }
      } catch (Exception ingored) {
      }

      if (newMZmineFile instanceof IMSRawDataFileImpl) {
        buildIonMobilityFile(msdkFile, newMZmineFile);
      } else {
        buildLCMSFile(msdkFile, newMZmineFile);
      }

    } catch (Throwable e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      return;
    }

    if (isCanceled()) {
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");

    newMZmineFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    project.addFile(newMZmineFile);

    setStatus(TaskStatus.FINISHED);
  }

  private double[][] applyMassDetection(MZmineProcessingStep<MassDetector> msDetector,
      MsdkScanWrapper scan) {
    // run mass detection on data object
    // [mzs, intensities]
    return msDetector.getModule().getMassValues(scan, msDetector.getParameterSet());
  }

  @Override
  public void cancel() {
    if (msdkTask != null) {
      msdkTask.cancel();
    }
    super.cancel();
  }

  public void buildLCMSFile(io.github.msdk.datamodel.RawDataFile file, RawDataFile newMZmineFile)
      throws IOException {
    for (MsScan scan : file.getScans()) {
      if (isCanceled()) {
        return;
      }
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;

      Scan newScan = null;
      if (applyMassDetection) {
        // wrap scan
        MsdkScanWrapper wrapper = new MsdkScanWrapper(scan);
        double[][] mzIntensities = null;

        // apply mass detection
        if (ms1Detector != null && wrapper.getMSLevel() == 1) {
          mzIntensities = applyMassDetection(ms1Detector, wrapper);
        } else if (ms2Detector != null && wrapper.getMSLevel() >= 2) {
          mzIntensities = applyMassDetection(ms2Detector, wrapper);
          if (denormalizeMSnScans) {
            ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mzIntensities[1],
                wrapper.getInjectionTime());
          }
        }

        if (mzIntensities != null) {
          // scans sorting is enforced in {@link AbstractStorableSpectrum#setDataPoints}
          // create mass list and scan. Override data points and spectrum type
          newScan = ConversionUtils.msdkScanToSimpleScan(newMZmineFile, mzMLScan, mzIntensities[0],
              mzIntensities[1], MassSpectrumType.CENTROIDED);
          ScanPointerMassList newMassList = new ScanPointerMassList(newScan);
          newScan.addMassList(newMassList);
        }
      }

      if (newScan == null) {
        // mz arrays are sorted within this method to ensure order in scans
        newScan = ConversionUtils.msdkScanToSimpleScan(newMZmineFile, mzMLScan);
      }

      newMZmineFile.addScan(newScan);
      parsedScans++;
      description =
          "Importing " + this.file.getName() + ", parsed " + parsedScans + "/" + totalScans
              + " scans";
    }
  }

  public void buildIonMobilityFile(io.github.msdk.datamodel.RawDataFile file,
      RawDataFile newMZmineFile) throws IOException {
    int mobilityScanNumberCounter = 0;
    int frameNumber = 1;
    SimpleFrame buildingFrame = null;

    final List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
    final List<BuildingImsMsMsInfo> buildingImsMsMsInfos = new ArrayList<>();
    Set<PasefMsMsInfo> finishedImsMsMsInfos;
    final IMSRawDataFile newImsFile = (IMSRawDataFile) newMZmineFile;

    // index ion mobility values first, some manufacturers don't save all scans for all frames if
    // they are empty.
    final RangeMap<Double, Integer> mappedMobilities = indexMobilityValues(file);
    final Map<Range<Double>, Integer> mobilitiesMap = mappedMobilities.asMapOfRanges();
    final double[] mobilities = mobilitiesMap.keySet().stream().mapToDouble(RangeUtils::rangeCenter)
        .toArray();

//    int previousFunction = 1;
    for (MsScan scan : file.getScans()) {
      if (isCanceled()) {
        return;
      }
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;
      if (mzMLScan.getMobility() == null) {
        continue;
      }
      if (mzMLScan.getMobility().mobilityType() == MobilityType.TIMS
          && mobilities[0] - mobilities[1] < 0) {
        // for tims, mobilities must be sorted in descending order, so if [0]-[1] < 0, we must reverse
        ArrayUtils.reverse(mobilities);
      }
      final Matcher watersMatcher = watersPattern.matcher(mzMLScan.getId());
      if (buildingFrame == null
          || Float.compare((scan.getRetentionTime() / 60f), buildingFrame.getRetentionTime())
          != 0 /*|| (watersMatcher.matches() && Integer.parseInt(watersMatcher.group(1)) != previousFunction)*/) {
//        previousFunction = watersMatcher.matches() ? Integer.parseInt(watersMatcher.group(1)) : 1;

        if (buildingFrame != null) { // finish the frame
          final SimpleFrame finishedFrame = buildingFrame;

          while (mobilityScanNumberCounter < mobilities.length) {
            mobilityScans.add(
                new BuildingMobilityScan(mobilityScanNumberCounter, MassDetector.EMPTY_DATA));
            mobilityScanNumberCounter++;
          }

          finishedFrame.setMobilityScans(mobilityScans, applyMassDetection);
          finishedFrame.setMobilities(mobilities);
          newImsFile.addScan(buildingFrame);

          mobilityScans.clear();
          // we need to reset if we start a new frame.
          mobilityScanNumberCounter = 0; // mobility scan numbers start with 0!
          if (!buildingImsMsMsInfos.isEmpty()) {
            finishedImsMsMsInfos = new HashSet<>();
            for (BuildingImsMsMsInfo info : buildingImsMsMsInfos) {
              finishedImsMsMsInfos.add(info.build(null, buildingFrame));
            }
            finishedFrame.setPrecursorInfos(finishedImsMsMsInfos);
          }
          buildingImsMsMsInfos.clear();
        }

        buildingFrame = new SimpleFrame(newImsFile, frameNumber, scan.getMsLevel(),
            scan.getRetentionTime() / 60f, null, null,
            ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
            ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
            scan.getScanningRange(), mzMLScan.getMobility().mobilityType(), null, null);
        frameNumber++;

        description =
            "Importing " + this.file.getName() + ", parsed " + parsedScans + "/" + totalScans
                + " scans";
      }

      // I'm not proud of this piece of code, but some manufactures or conversion tools leave out
      // empty scans. Looking at you, Agilent. however, we need that info for proper processing ~SteffenHeu
      Integer newScanId = mappedMobilities.get(mzMLScan.getMobility().mobility());
      final int missingScans = newScanId - mobilityScanNumberCounter;
      // might be negative in case of tims, but for now we assume that no scans missing for tims
      if (missingScans > 1) {
        for (int i = 0; i < missingScans; i++) {
          // make up for data saving options leaving out empty scans.
          mobilityScans.add(
              new BuildingMobilityScan(mobilityScanNumberCounter, MassDetector.EMPTY_DATA));
          mobilityScanNumberCounter++;
        }
      }

      mobilityScans.add(ConversionUtils.msdkScanToMobilityScan(mobilityScanNumberCounter, scan));
      ConversionUtils.extractImsMsMsInfo(mzMLScan, buildingImsMsMsInfos, frameNumber,
          mobilityScanNumberCounter);
      mobilityScanNumberCounter++;
      parsedScans++;
    }

    // apply mass detection to frames and mobility scans
    if (advancedParam != null) {
      logger.warning("""
          Applying the advanced import (with mass detection) to an IMS mzML file only performs mass
           detection on the summed frame level. Better to perform individual steps of mass detection
            to the mobility scans and the summed frames.""");
      MsDataImportAndMassDetectWrapperTask massDetector = new MsDataImportAndMassDetectWrapperTask(
          storage, newMZmineFile, this, parameters, moduleCallDate);
      massDetector.applyMassDetection();
    }
  }

  /**
   * Reads all mobility values in the file and returns a map of all mobilities with their scan
   * number.
   * <p></p>
   * The scan number for a given mobility value can be retrieved from the range map. The range map
   * is centered at the original mobility value with a quarter of the median difference between two
   * consecutive mobility values. (tims does not have the same difference between every mobility
   * scan, hence the quarter.)
   */
  private RangeMap<Double, Integer> indexMobilityValues(io.github.msdk.datamodel.RawDataFile file) {
    final RangeMap<Double, Integer> mobilityCounts = TreeRangeMap.create();

    boolean isTims = false;
    for (MsScan scan : file.getScans()) {
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;
      final Matcher matcher = watersPattern.matcher(mzMLScan.getId());
      if (matcher.matches() && !matcher.group(1).equals("1")) {
        continue;
      }
      isTims = mzMLScan.getMobility().mobilityType() == MobilityType.TIMS;

      final double mobility = mzMLScan.getMobility().mobility();
      final Entry<Range<Double>, Integer> entry = mobilityCounts.getEntry(mobility);
      if (entry == null) {
        final double delta = isTims ? 0.000002 : 0.00002;
        final Range<Double> range = SpectraMerging.createNewNonOverlappingRange(mobilityCounts,
            Range.closed(mobility - delta, mobility + delta));
        mobilityCounts.put(range, 1);
      } else {
        mobilityCounts.put(entry.getKey(), entry.getValue() + 1);
      }
    }

    final Map<Range<Double>, Integer> map = mobilityCounts.asMapOfRanges();
    final double[] mobilityValues = map.keySet().stream().mapToDouble(RangeUtils::rangeCenter)
        .toArray();
    final double[] diffs = new double[mobilityValues.length - 1];
    for (int i = 0; i < diffs.length; i++) {
      diffs[i] = mobilityValues[i + 1] - mobilityValues[i];
    }
    final double medianDiff = Quantiles.median().compute(diffs);
    final double tenthDiff = medianDiff / 10;
    RangeMap<Double, Integer> realMobilities = TreeRangeMap.create();
    for (int i = 0; i < mobilityValues.length; i++) {
      realMobilities.put(Range.closed(mobilityValues[i] - tenthDiff, mobilityValues[i] + tenthDiff),
          isTims ? mobilityValues.length - 1 - i : i); // reverse scan number order for tims
    }

    return realMobilities;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (msdkTask == null || msdkTask.getFinishedPercentage() == null) {
      return 0.0;
    }
    final double msdkProgress = msdkTask.getFinishedPercentage().doubleValue();
    final double parsingProgress = totalScans == 0 ? 0.0 : (double) parsedScans / totalScans;
    return (msdkProgress * 0.25) + (parsingProgress * 0.75);
  }
}
