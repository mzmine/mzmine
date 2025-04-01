/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.MsMsInfoType;
import io.github.mzmine.datamodel.features.types.numbers.RtMs2ApexDistanceType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.dataprocessing.filter_groupms2_refine.GroupedMs2RefinementProcessor;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.operations.AbstractTaskSubProcessor;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups fragmentation scans with features in range
 */
public class GroupMS2Processor extends AbstractTaskSubProcessor {

  public static final String DEFAULT_QUANT_FILE_COLUMN_NAME = "mainQuantFile";

  private static final Logger logger = Logger.getLogger(GroupMS2Processor.class.getName());

  private final MemoryMapStorage timsScanStorage;
  private final Double minMs2IntensityAbs;
  private final boolean combineTimsMS2;
  private final Double minMs2IntensityRel;
  private final FeatureList list;
  private final MZTolerance mzTol;
  private final boolean lockToFeatureMobilityRange;
  private final int minimumSignals;
  private final Double minimumRelativeFeatureHeight;
  private final int totalRows;
  private final RtLimitsFilter rtFilter;
  private final FragmentScanSelection timsFragmentScanSelection;
  private final int minImsDetections;
  private int processedRows;
  private GroupedMs2RefinementProcessor refineTask;
  private @NotNull String description = "";


  /**
   * Defines the column name that contains the file name of the data file that MS2s from other files
   * shall be assigned to. Eg. File A would be a MS1 only file and files B and C would be MS2 only
   * files. Files B and C would contain "File A" in their row in the mainQuantFileColumn.
   */
  private final String otherFileMs2MetadataColumnTitle;


  /**
   * msms info sorted by precursor mz
   */
  private final Map<RawDataFile, List<DDAMsMsInfo>> fileMs2Cache = new HashMap<>();

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Processor(@Nullable AbstractTask parentTask, final FeatureList list,
      final ParameterSet parameterSet) {
    this(parentTask, list, parameterSet, ((ModularFeatureList) list).getMemoryMapStorage());
  }

  public GroupMS2Processor(@Nullable AbstractTask parentTask, final FeatureList list,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage timsScanStorage) {
    super(parentTask);

    this.timsScanStorage = timsScanStorage;
    // RT has two options / tolerance is only provided for second option
    rtFilter = parameterSet.getValue(GroupMS2Parameters.rtFilter);

    mzTol = parameterSet.getValue(GroupMS2Parameters.mzTol);
    combineTimsMS2 = parameterSet.getValue(GroupMS2Parameters.combineTimsMsMs);
    lockToFeatureMobilityRange = parameterSet.getValue(GroupMS2Parameters.limitMobilityByFeature);
    minImsDetections = parameterSet.getValue(GroupMS2Parameters.minImsRawSignals);

    final ParameterSet advancedParam = parameterSet.getEmbeddedParameterValue(
        GroupMS2Parameters.advancedParameters);
    final Boolean advancedSelected = parameterSet.getValue(GroupMS2Parameters.advancedParameters);

    otherFileMs2MetadataColumnTitle =
        advancedSelected ? advancedParam.getEmbeddedParameterValueIfSelectedOrElse(
            GroupMs2AdvancedParameters.iterativeMs2Column, null) : null;

    minMs2IntensityAbs = advancedSelected ? advancedParam.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMs2AdvancedParameters.outputNoiseLevel, null) : null;
    minMs2IntensityRel = advancedSelected ? advancedParam.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMs2AdvancedParameters.outputNoiseLevelRelative, null) : null;

    // if active, only features with min relative height get MS2
    minimumRelativeFeatureHeight = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.minimumRelativeFeatureHeight, null);

    // 0 is deactivated
    minimumSignals = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.minRequiredSignals, 0);

    // only used for tims, keeping input spectra is important for later merging.
    var scanTypes = List.of(MergedSpectraFinalSelectionTypes.EACH_SAMPLE,
        MergedSpectraFinalSelectionTypes.EACH_ENERGY);
    var merger = new SpectraMerger(scanTypes, SpectraMerging.pasefMS2MergeTol,
        IntensityMergingType.MAXIMUM);
    timsFragmentScanSelection = new FragmentScanSelection(timsScanStorage,
        SelectInputScans.ALL_SCANS, merger, scanTypes);

    this.list = list;
    processedRows = 0;
    totalRows = list.getNumberOfRows();
    description = "Grouped MS2 scans in list " + list.getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (refineTask != null) {
      return refineTask.getFinishedPercentage();
    }
    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public @NotNull String getTaskDescription() {
    if (refineTask != null) {
      return refineTask.getTaskDescription();
    }
    return description;
  }

  @Override
  public void process() {
    // for all features
    for (FeatureListRow row : list.getRows()) {
      if (isCanceled()) {
        return;
      }

      processRow(row);
      processedRows++;
    }

    // refine MS2 groupings with features that are at least X % of the highest feature that was grouped with each MS2
    if (minimumRelativeFeatureHeight != null) {
      refineTask = new GroupedMs2RefinementProcessor(parentTask, list, minimumRelativeFeatureHeight,
          0d);
      refineTask.process();
    }
  }

  /**
   * Group all MS2 scans with the corresponding features (per raw data file)
   *
   * @param row does this for each feature in this row
   */
  private void processRow(FeatureListRow row) {
    for (ModularFeature feature : row.getFeatures()) {
      List<Scan> scans;
      if (MobilityType.TIMS.isTypeOfBackingRawData(feature)) {
        scans = findFragmentScansForTimsFeature(feature);
      } else {
        scans = findFragmentScans(feature);
      }

      scans = filterByMinimumSignals(scans);
      feature.setAllMS2FragmentScans(scans.isEmpty() ? null : scans, true);
      // get proximity
      setRtApexProximity(feature, scans);
    }
  }

  /**
   * Find all fragment scans for this feature applying RT and mz filters
   *
   * @return list of fragment scans
   */
  @NotNull
  private List<Scan> findFragmentScans(final ModularFeature feature) {
    RawDataFile raw = feature.getRawDataFile();

    final List<DDAMsMsInfo> msmsInfos = fileMs2Cache.computeIfAbsent(raw, r -> {
      final Stream<DDAMsMsInfo> ms2sSameFile = r.getScans().stream()
          .filter(s -> s.getMSLevel() >= 2 && s.getMsMsInfo() instanceof DDAMsMsInfo)
          .map(s -> (DDAMsMsInfo) s.getMsMsInfo());
      final Stream<DDAMsMsInfo> ms2InfosInOtherFiles = findMs2InfosInOtherFiles(r);
      return Stream.concat(ms2sSameFile, ms2InfosInOtherFiles)
          .sorted(Comparator.comparingDouble(DDAMsMsInfo::getIsolationMz))
          .collect(Collectors.toList());
    });

    final IndexRange indexRange = BinarySearch.indexRange(mzTol.getToleranceRange(feature.getMZ()),
        msmsInfos, DDAMsMsInfo::getIsolationMz);
    final List<Scan> ms2s = indexRange.sublist(msmsInfos).stream().map(DDAMsMsInfo::getMsMsScan)
        .filter(Objects::nonNull).filter(s -> filterScan(s, feature))
        .sorted(FragmentScanSorter.DEFAULT_TIC).toList();

    return ms2s;
  }

  /**
   * Searches for MS2 infos in other data files, e.g. generated by iterative exclusion or acquire x.
   * Note that the stream is not sorted in any way.
   */
  @NotNull
  private Stream<DDAMsMsInfo> findMs2InfosInOtherFiles(@Nullable final RawDataFile mainFile) {
    if (mainFile == null) {
      return Stream.empty();
    }
    final MetadataTable metadata = ProjectService.getMetadata();
    final MetadataColumn<String> mainQuantFileCol = (MetadataColumn<String>) metadata.getColumnByName(
        otherFileMs2MetadataColumnTitle);
    if (mainQuantFileCol == null) {
      return Stream.empty();
    }
    // compare without format
    final String mainFileNameNoFormat = FileAndPathUtil.eraseFormat(mainFile.getName());

    final Map<String, List<RawDataFile>> filesByQuantFileNames = metadata.groupFilesByColumn(
        mainQuantFileCol);
    final List<RawDataFile> ms2Files = filesByQuantFileNames.entrySet().stream()
        .filter((quantFileEntry) -> {
          final String quantFileNameNoFormat = FileAndPathUtil.eraseFormat(quantFileEntry.getKey());
          return quantFileNameNoFormat.equalsIgnoreCase(mainFileNameNoFormat);
        }).flatMap(e -> e.getValue().stream()).toList();

    return ms2Files.stream().flatMap(file -> file.getScans().stream()).map(Scan::getMsMsInfo)
        .filter(DDAMsMsInfo.class::isInstance).map(DDAMsMsInfo.class::cast);
  }

  /**
   * Calculate and set the RT proximity
   *
   * @param f     feature
   * @param scans feature's fragment scans
   */
  private void setRtApexProximity(final ModularFeature f, final List<Scan> scans) {
    if (scans.isEmpty()) {
      return;
    }
    float apexDistance = Float.MAX_VALUE;
    for (Scan s : scans) {
      float dist = s.getRetentionTime() - f.getRT();
      if (dist < apexDistance) {
        apexDistance = dist;
      }
    }
    f.set(RtMs2ApexDistanceType.class, apexDistance);
  }

  /**
   * Filter scans based on rt and mz
   *
   * @param scan tested scan
   * @return true if matches all criteria
   */
  private boolean filterScan(Scan scan, ModularFeature feature) {
    if (scan.getPolarity() != feature.getRepresentativePolarity()) {
      return false;
    }
    // minimum signals
    if (minimumSignals > 0) {
      MassList massList = scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(scan);
      }
      if (massList.getNumberOfDataPoints() < minimumSignals) {
        return false;
      }
    }
    //
    final double precursorMZ;
    if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
      precursorMZ = msn.getMS2PrecursorMz();
    } else if (scan.getMsMsInfo() instanceof DDAMsMsInfo info) {
      precursorMZ = info.getIsolationMz();
    } else {
      precursorMZ = Objects.requireNonNullElse(scan.getPrecursorMz(), 0d);
    }
    return rtFilter.accept(feature, scan.getRetentionTime()) && precursorMZ != 0
           && mzTol.checkWithinTolerance(feature.getMZ(), precursorMZ);
  }


  /**
   * Process tims features. Merge within Frames and optionally merge across frames
   *
   * @param feature feature from TIMS data
   * @return list of fragmentation scans
   */
  @NotNull
  private List<Scan> findFragmentScansForTimsFeature(ModularFeature feature) {

    final double fmz = feature.getMZ();
    final Float mobility = feature.getMobility();
    final IMSRawDataFile raw = (IMSRawDataFile) feature.getRawDataFile();

    final List<DDAMsMsInfo> msmsInfos = fileMs2Cache.computeIfAbsent(raw,
        _ -> raw.getFrames().stream().filter(
                s -> s.getMSLevel() >= 2 && s.getImsMsMsInfos() instanceof Set<?> set && !set.isEmpty())
            .flatMap(s -> s.getImsMsMsInfos().stream()).filter(info -> info instanceof DDAMsMsInfo)
            .map(info -> (DDAMsMsInfo) info)
            .sorted(Comparator.comparingDouble(DDAMsMsInfo::getIsolationMz)).toList());

    final IndexRange indexRange = BinarySearch.indexRange(mzTol.getToleranceRange(feature.getMZ()),
        msmsInfos, DDAMsMsInfo::getIsolationMz);
    final List<PasefMsMsInfo> infos = indexRange.sublist(msmsInfos).stream()
        .filter(PasefMsMsInfo.class::isInstance).map(PasefMsMsInfo.class::cast).toList();

    if (infos.isEmpty()) {
      return List.of();
    }

    final List<? extends PasefMsMsInfo> eligibleMsMsInfos = infos.stream()
        .<PasefMsMsInfo>mapMulti((imsMsMsInfo, c) -> {
          if (mzTol.checkWithinTolerance(fmz, imsMsMsInfo.getIsolationMz()) && rtFilter.accept(
              feature, imsMsMsInfo.getMsMsFrame().getRetentionTime())) {
            final Frame frame = (Frame) imsMsMsInfo.getMsMsScan();
            // if we have a mobility (=processed by IMS workflow), we can check for the correct range during assignment.
            if (mobility != null) {
              // todo: maybe revisit this for a more sophisticated range check
              int mobilityScannumberOffset = frame.getMobilityScan(0).getMobilityScanNumber();
              float mobility1 = (float) frame.getMobilityForMobilityScanNumber(
                  imsMsMsInfo.getSpectrumNumberRange().lowerEndpoint() - mobilityScannumberOffset);
              float mobility2 = (float) frame.getMobilityForMobilityScanNumber(
                  imsMsMsInfo.getSpectrumNumberRange().upperEndpoint() - mobilityScannumberOffset);
              if (Range.singleton(mobility1).span(Range.singleton(mobility2)).contains(mobility)) {
                c.accept(imsMsMsInfo);
              }
            } else {
              // if we don't have a mobility, we can simply add the msms info.
              c.accept(imsMsMsInfo);
            }
          }
        }).toList();

    if (eligibleMsMsInfos.isEmpty()) {
      return List.of();
    }
    feature.set(MsMsInfoType.class, (List<MsMsInfo>) (List<? extends MsMsInfo>) eligibleMsMsInfos);

    List<Scan> msmsSpectra = new ArrayList<>();
    for (PasefMsMsInfo info : eligibleMsMsInfos) {
      Range<Float> mobilityLimits = lockToFeatureMobilityRange && feature.getMobilityRange() != null
          ? feature.getMobilityRange() : null;
      MergedMsMsSpectrum spectrum = SpectraMerging.getMergedMsMsSpectrumForPASEF(info,
          SpectraMerging.pasefMS2MergeTol, IntensityMergingType.SUMMED, timsScanStorage,
          mobilityLimits, minMs2IntensityAbs, minMs2IntensityRel, minImsDetections);
      if (spectrum != null) {
        msmsSpectra.add(spectrum);
      }
    }

    if (!msmsSpectra.isEmpty() && combineTimsMS2) {
      return timsFragmentScanSelection.getAllFragmentSpectra(msmsSpectra);
    }
    return msmsSpectra;
  }


  /**
   * remove all scans with less than minimumSignals in mass list
   *
   * @param scans returns a filtered list or the input list if no filter is applied
   */
  private List<Scan> filterByMinimumSignals(final List<Scan> scans) {
    if (minimumSignals <= 0) {
      return scans;
    }

    for (final Scan scan : scans) {
      if (scan.getMassList() == null) {
        throw new MissingMassListException(scan);
      }
    }

    return scans.stream()
        .filter(scan -> scan.getMassList().getNumberOfDataPoints() >= minimumSignals).toList();
  }
}
