/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.math.Quantiles;
import io.github.msdk.MSDKException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.BuildingMobilityScanStorage;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.BuildingMzMLMobilityScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.BuildingMzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLParser;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLRawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.date.DateTimeUtils;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class reads mzML 1.0 and 1.1.0 files (<a
 * href="http://www.psidev.info/index.php?q=node/257">http://www.psidev.info/index.php?q=node/257</a>)
 * using the jmzml library (<a
 * href="http://code.google.com/p/jmzml/">http://code.google.com/p/jmzml/</a>).
 */
@SuppressWarnings("UnstableApiUsage")
public class MSDKmzMLImportTask extends AbstractTask implements RawDataImportTask {

  public static final Pattern watersPattern = Pattern.compile(
      "function=([1-9]+) process=([0-9]+) scan=([0-9]+)");
  private static final Logger logger = Logger.getLogger(MSDKmzMLImportTask.class.getName());

  // File is always set even if the input stream may be already opened, e.g., from a converter
  private final @NotNull File file;
  private final @Nullable InputStream fis;
  // advanced processing will apply mass detection directly to the scans
  private final MZmineProject project;
  private final @NotNull ScanImportProcessorConfig scanProcessorConfig;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private int totalScansAfterFilter = 0, convertedScansAfterFilter;
  private String description;

  private MzMLParser parser;
  private RawDataFileImpl newMZmineFile;

  /**
   * Create for file
   */
  public MSDKmzMLImportTask(MZmineProject project, @NotNull File fileToOpen,
      @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    this(project, fileToOpen, null, scanProcessorConfig, module, parameters, moduleCallDate,
        storage);
  }

  /**
   * Create for input stream
   *
   * @param fisToOpen         file input stream defines the input
   * @param fileOfInputStream is the input stream origin but is not directly used for import here
   */
  public MSDKmzMLImportTask(MZmineProject project, @NotNull File fileOfInputStream,
      @Nullable InputStream fisToOpen, @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.file = fileOfInputStream;
    this.fis = fisToOpen;
    this.project = project;
    description = "Importing raw data file: " + fileOfInputStream.getName();
    this.scanProcessorConfig = scanProcessorConfig;
    this.parameters = parameters;
    this.module = module;
  }

  private static boolean isExcludedWatersScan(final BuildingMzMLMobilityScan mzMLScan) {
    final Matcher matcher = watersPattern.matcher(mzMLScan.id());
    if (matcher.matches() && !matcher.group(1).equals("1")) {
      return true;
    }
    return false;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    RawDataFile dataFile = importStreamOrFile();

    if (dataFile == null || isCanceled()) {
      return;
    }

    addAppliedMethodAndAddToProject(dataFile);

    if (convertedScansAfterFilter == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  public void addAppliedMethodAndAddToProject(final RawDataFile dataFile) {
    dataFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    project.addFile(dataFile);
  }

  /**
   * Import mzml from InputStream (if not null) or from file otherwise. Does not add the applied
   * method and does not add the data file to project
   *
   * @return the raw data file, only if successful and not canceled
   */
  @Nullable
  public RawDataFile importStreamOrFile() {
    try {
      MzMLRawDataFile msdkTaskRes = parseMzMl();

      if (isCanceled()) {
        return null;
      }

      if (msdkTaskRes == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("MSDK returned null");
        return null;
      }

      var startTimeStamp = DateTimeUtils.parseOrElse(msdkTaskRes.getStartTimeStamp(), null);

      final boolean isIms = !msdkTaskRes.getMobilityScanData().isEmpty();

      if (isIms) {
        totalScansAfterFilter = msdkTaskRes.getMobilityScanData().size();
        newMZmineFile = buildIonMobilityFile(msdkTaskRes);
      } else {
        totalScansAfterFilter = msdkTaskRes.getMsScans().size();
        newMZmineFile = buildLCMSFile(msdkTaskRes);
      }
      if (isCanceled() || newMZmineFile == null) {
        return null;
      }

      final List<OtherDataFile> otherDataFiles = ConversionUtils.convertOtherSpectra(newMZmineFile,
          msdkTaskRes.getOtherSpectra());
      final List<OtherDataFile> otherTraceFiles = ConversionUtils.convertOtherTraces(newMZmineFile,
          msdkTaskRes.getChromatograms());
      newMZmineFile.addOtherDataFiles(otherDataFiles);
      newMZmineFile.addOtherDataFiles(otherTraceFiles);

      newMZmineFile.setStartTimeStamp(startTimeStamp);
      logger.info("Finished parsing " + file + ", parsed " + convertedScansAfterFilter + " scans");

      if (totalScansAfterFilter == 0 && newMZmineFile.getOtherDataFiles().isEmpty()) {
        var activeFilter = scanProcessorConfig.scanFilter().isActiveFilter();
        String filter = activeFilter ? """
            Scan filters were active in import and filtered out %d scans,
            either deactivate the filters or remove this file from the import list""".formatted(
            getTotalScansInMzML()) : "Scan filters were off.";

        String msg = "%s had 0 scans after import. %s".formatted(file.getName(), filter);
        DialogLoggerUtil.showMessageDialogForTime("Empty file", msg);
      }

      return newMZmineFile;
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Error during mzML read: " + e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      return null;
    }
  }

  public RawDataFileImpl buildLCMSFile(MzMLRawDataFile file) throws IOException {
    String descriptionTemplate = description = "Importing %s, total / parsed is %d / ".formatted(
        this.file.getName(), totalScansAfterFilter);
    RawDataFileImpl newMZmineFile = new RawDataFileImpl(this.file.getName(),
        this.file.getAbsolutePath(), storage);

    List<BuildingMzMLMsScan> msScans = file.getMsScans();
    if (!areScansSorted(msScans)) {
      msScans = msScans.stream()
          .sorted(Comparator.comparingDouble(BuildingMzMLMsScan::getRetentionTime)).toList();
      AtomicInteger scanNumber = new AtomicInteger(1);
      msScans.forEach(scan -> scan.setScanNumber(scanNumber.getAndIncrement()));
    }

    for (BuildingMzMLMsScan mzMLScan : msScans) {
      if (isCanceled()) {
        return newMZmineFile;
      }

      Scan newScan = convertScan(mzMLScan, newMZmineFile);
      newMZmineFile.addScan(newScan);

      convertedScansAfterFilter++;
      description = descriptionTemplate + convertedScansAfterFilter;
    }
    return newMZmineFile;
  }

  private boolean areScansSorted(List<BuildingMzMLMsScan> msScans) {
    for (int i = 1; i < msScans.size(); i++) {
      if (!(msScans.get(i).getRetentionTime() > msScans.get(i - 1).getRetentionTime()) || !(
          msScans.get(i).getScanNumber() > msScans.get(i - 1).getScanNumber())) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  private Scan convertScan(final BuildingMzMLMsScan mzMLScan, final RawDataFileImpl newMZmineFile) {
    // might not be centroided if mass detection was off
    if (scanProcessorConfig.isMassDetectActive(mzMLScan.getMSLevel())) {
      Scan scan = ConversionUtils.mzmlScanToSimpleScan(newMZmineFile, mzMLScan,
          MassSpectrumType.CENTROIDED);
      scan.addMassList(new ScanPointerMassList(scan));
      return scan;
    } else {
      return ConversionUtils.mzmlScanToSimpleScan(newMZmineFile, mzMLScan);
    }
  }

  public IMSRawDataFileImpl buildIonMobilityFile(MzMLRawDataFile file) throws IOException {
    String descriptionTemplate = description = "Importing %s, total / parsed is %d / ".formatted(
        this.file.getName(), totalScansAfterFilter);
    IMSRawDataFileImpl newImsFile = new IMSRawDataFileImpl(this.file.getName(),
        this.file.getAbsolutePath(), storage);

    // index ion mobility values first, some manufacturers don't save all scans for all frames if
    // they are empty.
    final RangeMap<Double, Integer> mappedMobilities = indexMobilityValues(file);
    final Map<Range<Double>, Integer> mobilitiesMap = mappedMobilities.asMapOfRanges();
    final double[] mobilities = mobilitiesMap.keySet().stream().mapToDouble(RangeUtils::rangeCenter)
        .toArray();

    // each element is one frame and all its mobility scans
    // already memory mapped - for each frame
    List<BuildingMobilityScanStorage> framesMobilityScans = file.getMobilityScanData();

    // reverse mobilities for TIMS
    // all this is checked before when building the BuildingMobilityScanStorage
    var mobilityType = framesMobilityScans.getFirst().getMobilityScans().getFirst().mobilityType();

    // TIMS may have multiple segments but people should just use tdf
    if (mobilityType == MobilityType.TIMS && mobilities[0] - mobilities[1] < 0) {
      // for tims, mobilities must be sorted in descending order, so if [0]-[1] < 0, we must reverse
      ArrayUtils.reverse(mobilities);
    }

    int frameNumber = 1;
    for (final BuildingMobilityScanStorage frameStorage : framesMobilityScans) {
      if (isCanceled()) {
        return newImsFile;
      }

      buildFrame(newImsFile, frameNumber, frameStorage, mobilities, mappedMobilities, mobilityType);
      frameNumber++;
      convertedScansAfterFilter++;
      description = descriptionTemplate + convertedScansAfterFilter;
    }

    // Mass detection is automatically applied to mobility scans if advanced parameters are selected
    // at this point we do not have frames - so mass detection cannot be applied
    // in future we need to find an easy way to get a simple frame already calculated from the mobility scans
    // currently users need to apply merging step
    if (scanProcessorConfig.processor().containsMassDetection()) {
      logger.info("""
          Advanced data import applied mass detection on mobility scans.""");
//      MsDataImportAndMassDetectWrapperTask massDetector = new MsDataImportAndMassDetectWrapperTask(
//          storage, newImsFile, this, scanProcessorConfig, moduleCallDate);
//      massDetector.applyMassDetection();
    }
    return newImsFile;
  }

  private void buildFrame(final IMSRawDataFileImpl newImsFile, final int frameNumber,
      final BuildingMobilityScanStorage frameStorage, final double[] mobilities,
      final RangeMap<Double, Integer> mappedMobilities, final MobilityType mobilityType)
      throws IOException {
    var scans = frameStorage.getMobilityScans();

    final List<BuildingImsMsMsInfo> buildingImsMsMsInfos = new ArrayList<>();
    Set<IonMobilityMsMsInfo> finishedImsMsMsInfos;
    int mobilityScanNumberCounter = 0;

    int[] storageOffsets = new int[mobilities.length];
    int[] basePeakIndices = new int[mobilities.length];

    for (int scanIndex = 0; scanIndex < scans.size(); scanIndex++) {
      final BuildingMzMLMobilityScan mzMLScan = scans.get(scanIndex);
      int storageOffset = frameStorage.getStorageOffset(scanIndex);
      int basePeakIndex = frameStorage.getBasePeakIndex(scanIndex);

      // start msms info construction here in case there are missing scans
      ConversionUtils.extractImsMsMsInfo(mzMLScan.precursorList(), buildingImsMsMsInfos,
          frameNumber, mobilityScanNumberCounter);

      // fill in missing scans
      // I'm not proud of this piece of code, but some manufactures or conversion tools leave out
      // empty scans. Looking at you, Agilent. however, we need that info for proper processing ~SteffenHeu
      Integer newScanId = mappedMobilities.get(mzMLScan.mobility());
      final int missingScans = newScanId - mobilityScanNumberCounter;
      // might be negative in case of tims, but for now we assume that no scans missing for tims
      for (int i = 0; i < missingScans; i++) {
        // make up for data saving options leaving out empty scans.
        storageOffsets[mobilityScanNumberCounter] = storageOffset;
        basePeakIndices[mobilityScanNumberCounter] = -1;
        mobilityScanNumberCounter++;

        // keep incrementing msms info construction here for missing scans
        ConversionUtils.extractImsMsMsInfo(mzMLScan.precursorList(), buildingImsMsMsInfos,
            frameNumber, mobilityScanNumberCounter);
      }

      storageOffsets[mobilityScanNumberCounter] = storageOffset;
      basePeakIndices[mobilityScanNumberCounter] = basePeakIndex;
      mobilityScanNumberCounter++;
    }

    // add trailing missing scans
    for (; mobilityScanNumberCounter < mobilities.length; mobilityScanNumberCounter++) {
//      mobilityScans.add(
//          new BuildingMobilityScan(mobilityScanNumberCounter, MassDetector.EMPTY_DATA));
      storageOffsets[mobilityScanNumberCounter] = storageOffsets[mobilityScanNumberCounter - 1];
      basePeakIndices[mobilityScanNumberCounter] = -1;
      if (!buildingImsMsMsInfos.isEmpty()) {
        buildingImsMsMsInfos.getLast().setLastSpectrumNumber(mobilityScanNumberCounter);
      }
    }

    SimpleFrame finishedFrame = frameStorage.createFrame(newImsFile, frameNumber);
    finishedFrame.setMobilities(mobilities);

    //
    boolean massDetectActive = scanProcessorConfig.isMassDetectActive(finishedFrame.getMSLevel());

    var mobilityScanStorage = new MobilityScanStorage(storage, finishedFrame,
        frameStorage.getMzValues(), frameStorage.getIntensityValues(),
        frameStorage.getMaxNumPoints(), storageOffsets, basePeakIndices, massDetectActive);

    finishedFrame.setMobilityScanStorage(mobilityScanStorage);
    newImsFile.addScan(finishedFrame);

    if (!buildingImsMsMsInfos.isEmpty()) {
      finishedImsMsMsInfos = new HashSet<>();
      for (BuildingImsMsMsInfo info : buildingImsMsMsInfos) {
        finishedImsMsMsInfos.add(info.build(null, finishedFrame));
      }
      finishedFrame.setPrecursorInfos(finishedImsMsMsInfos);
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
  private RangeMap<Double, Integer> indexMobilityValues(MzMLRawDataFile file) {
    final RangeMap<Double, Integer> mobilityCounts = TreeRangeMap.create();

    final AtomicBoolean isTimsEx = new AtomicBoolean(false);
    file.getMobilityScanData().stream().map(BuildingMobilityScanStorage::getMobilityScans)
        .flatMap(Collection::stream).filter(scan -> !isExcludedWatersScan(scan))
        .forEach(mzMLScan -> {

          boolean isTims = mzMLScan.mobilityType() == MobilityType.TIMS;
          isTimsEx.set(isTims);

          final double mobility = mzMLScan.mobility();
          final Entry<Range<Double>, Integer> entry = mobilityCounts.getEntry(mobility);
          if (entry == null) {
            final double delta = isTims ? 0.000002 : 0.00002;
            final Range<Double> range = SpectraMerging.createNewNonOverlappingRange(mobilityCounts,
                Range.closed(mobility - delta, mobility + delta));
            mobilityCounts.put(range, 1);
          } else {
            mobilityCounts.put(entry.getKey(), entry.getValue() + 1);
          }
        });

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
          isTimsEx.get() ? mobilityValues.length - 1 - i : i); // reverse scan number order for tims
    }

    return realMobilities;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    if (parser == null) {
      return 0.0;
    }
    final double msdkProgress = parser.getFinishedPercentage();
    final double parsingProgress = totalScansAfterFilter == 0 ? 0.0
        : (double) convertedScansAfterFilter / totalScansAfterFilter;
    return (msdkProgress * 0.95) + (parsingProgress * 0.05);
  }


  /**
   * This is the number of total scans after scan filtering applied
   */
  public int getTotalScansAfterFilter() {
    return totalScansAfterFilter;
  }

  /**
   * This is the number of already converted scans, after filtering
   */
  public int getConvertedScansAfterFilter() {
    return convertedScansAfterFilter;
  }

  /**
   * This is the number of total scans in mzML without filtering
   */
  public int getTotalScansInMzML() {
    if (parser == null) {
      return 0;
    }
    return parser.getTotalScans();
  }

  /**
   * THis is the number of parsed mzML scans, without filtering
   */
  public int getParsedMzMLScans() {
    if (parser == null) {
      return 0;
    }
    return parser.getParsedScans();
  }

// actual parsing

  /**
   * Parse the MzML data and return the parsed data
   *
   * @return a {@link MzMLRawDataFile MzMLRawDataFile} object containing the parsed data
   */
  public MzMLRawDataFile parseMzMl() throws MSDKException {
    try {
      // comparison of woodstox and aalto:
      // woodstox seems to use less memory
      // aalto seems to be a bit faster
      // both very similar
      // most memory is consumed by data reading and decompression etc
      // woodstox
//      XMLInputFactory2 factory = (XMLInputFactory2) XMLInputFactory2.newFactory();
//      factory.configureForSpeed();

      // aalto
      InputFactoryImpl factory = new InputFactoryImpl();
      factory.configureForSpeed();

      if (fis != null) {
        logger.finest("Began parsing file from stream");
        try (Reader reader = new InputStreamReader(fis)) {
          // buffered reader had no performance gains. most likely because the XMLStreamReader already buffers
//        BufferedReader br = new BufferedReader(reader, 8192*4);
          XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(reader);
          return parseMzMlInternal(xmlStreamReader);
        }
      } else if (file != null) {
        logger.finest("Began parsing file: " + file.getAbsolutePath());
        // buffered reader had no performance gains. most likely because the XMLStreamReader already buffers
//        try (BufferedReader br = Files.newBufferedReader(mzMLFile.toPath(),
        try (var fis = Files.newInputStream(file.toPath()); Reader br = new InputStreamReader(fis,
            StandardCharsets.UTF_8)) {
          XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(br);
          return parseMzMlInternal(xmlStreamReader);
        }
      } else {
        throw new MSDKException("Invalid input");
      }
    } catch (IOException | XMLStreamException ex) {
      throw new RuntimeException(ex);
    }
  }

  private MzMLRawDataFile parseMzMlInternal(XMLStreamReader xmlStreamReader) throws MSDKException {
    try {
      this.parser = new MzMLParser(this, storage, scanProcessorConfig);

      int eventType;
      try {
        do {
          // check if parsing has been cancelled?
          if (isCanceled()) {
            return null;
          }

          eventType = xmlStreamReader.next();

          switch (eventType) {
            case XMLStreamConstants.START_ELEMENT -> {
              final String openingTagName = xmlStreamReader.getLocalName();
              parser.processOpeningTag(xmlStreamReader, openingTagName);
            }
            case XMLStreamConstants.END_ELEMENT -> {
              final String closingTagName = xmlStreamReader.getLocalName();
              parser.processClosingTag(xmlStreamReader, closingTagName);
            }

//            processCharacters method is not used in the moment
//            might be returned if new random access xml parser is introduced
//            case XMLStreamConstants.CHARACTERS:
//              parser.processCharacters(xmlStreamReader);
//              break;
          }

        } while (eventType != XMLStreamConstants.END_DOCUMENT);

      } catch (DataFormatException | XMLStreamException e) {
        throw new RuntimeException(e);
      }
      logger.finest("Parsing Complete");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error while loading mzML/RAW file " + e.getMessage(), e);
      throw (new MSDKException(e));
    }
    return parser.getMzMLRawFile();
  }

  public File getMzMLFile() {
    return file;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? newMZmineFile : null;
  }
}
