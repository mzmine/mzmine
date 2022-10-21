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

package io.github.mzmine.modules.dataprocessing.filter_maldigroupms2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class MaldiGroupMS2Task extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(MaldiGroupMS2Task.class.getName());
  // Feature lists.
  private final MZmineProject project;
  // Parameters.
  private final ParameterSet parameters;
  private final Double minMs2Intensity;
  private final boolean combineTimsMS2;
  private final List<IMSImagingRawDataFile> files;
  private final Double minMs2IntensityRel;
  // Processed rows counter
  private int processedRows, totalRows;
  private ModularFeatureList list;
  private MZTolerance mzTol;
  private final boolean lockToFeatureMobilityRange;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public MaldiGroupMS2Task(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    mzTol = parameters.getParameter(MaldiGroupMS2Parameters.mzTol).getValue();
    combineTimsMS2 = parameterSet.getParameter(MaldiGroupMS2Parameters.combineTimsMsMs).getValue();
    lockToFeatureMobilityRange = parameterSet.getParameter(
        MaldiGroupMS2Parameters.lockMS2ToFeatureMobilityRange).getValue();
    minMs2Intensity = parameterSet.getParameter(MaldiGroupMS2Parameters.outputNoiseLevel).getValue()
        ? parameterSet.getParameter(MaldiGroupMS2Parameters.outputNoiseLevel).getEmbeddedParameter()
        .getValue() : null;

    minMs2IntensityRel =
        parameterSet.getParameter(MaldiGroupMS2Parameters.outputNoiseLevelRelative).getValue()
            ? parameterSet.getParameter(MaldiGroupMS2Parameters.outputNoiseLevelRelative)
            .getEmbeddedParameter().getValue() : null;

    this.list = (ModularFeatureList) list;
    files = Arrays.stream(parameterSet.getParameter(MaldiGroupMS2Parameters.files).getValue()
            .getMatchingRawDataFiles()).filter(file -> file instanceof IMSImagingRawDataFile)
        .map(file -> (IMSImagingRawDataFile) file).toList();
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Adding all MS2 scans to their features in list " + list.getName();
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);

      totalRows = list.getNumberOfRows();

      // we need to create a new feature list, because the raw data files cannot be altered.
      // however, we need to set the raw data files so a project can be loaded/saved
      // LinkedHashSet to keep the current files at the front
//      final Set<RawDataFile> allFiles = new LinkedHashSet<>();
//      allFiles.addAll(list.getRawDataFiles());
//      allFiles.addAll(files);

      final ModularFeatureList newFlist = list;
//          list.createCopy(list.getName(), getMemoryMapStorage(),
//          allFiles.stream().toList(), false);
//      files.forEach(file -> newFlist.setSelectedScans(file, file.getScans()));

      // for all features
      for (FeatureListRow row : newFlist.getRows()) {
        if (isCanceled()) {
          return;
        }

        processRow(row);
        processedRows++;
      }

      newFlist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(MaldiGroupMS2Module.class, parameters,
              getModuleCallDate()));

//      project.removeFeatureList(list);
//      project.addFeatureList(newFlist);

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished adding all MS2 scans to their features in " + list.getName());

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Error while adding all MS2 scans to their feautres", t);
    }
  }

  /**
   * Group all MS2 scans with the corresponding features (per raw data file)
   *
   * @param row
   */
  public void processRow(FeatureListRow row) {
    for (ModularFeature f : row.getFeatures()) {
      if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN && (
          f.getMobilityUnit() == MobilityType.TIMS || (
              f.getRawDataFile() instanceof IMSRawDataFile imsfile
                  && imsfile.getMobilityType() == MobilityType.TIMS))) {
        processTimsFeature(f);
      }
    }
  }

  private void processTimsFeature(ModularFeature feature) {

    double fmz = feature.getMZ();
    Float mobility = feature.getMobility();

    // collect all frames with mslevel=2 with the same spot from all files
    final List<ImagingFrame> frames = files.stream()
        .flatMap(file -> file.getScanNumbers(2).stream().<ImagingFrame>mapMulti((scan, c) -> {
          if (!(scan instanceof ImagingFrame imgFrame) || imgFrame.getMaldiSpotInfo() == null) {
            return;
          }
          final MaldiSpotInfo maldiSpotInfo = imgFrame.getMaldiSpotInfo();
          final String spotName = maldiSpotInfo.spotName();

          if (feature.getFeatureData().getSpectra().stream().anyMatch(
              (frame -> ((ImagingFrame) frame).getMaldiSpotInfo().spotName().equals(spotName)))) {
            c.accept(imgFrame);
          }
        })).toList();

    if (frames.isEmpty()) {
      return;
    }

    final List<MsMsInfo> eligibleMsMsInfos = new ArrayList<>();
    for (Frame frame : frames) {
      frame.getImsMsMsInfos().forEach(imsMsMsInfo -> {
        if (mzTol.checkWithinTolerance(fmz, imsMsMsInfo.getIsolationMz())) {
          // if we have a mobility (=processed by IMS workflow), we can check for the correct range during assignment.
          if (mobility != null) {
            // todo: maybe revisit this for a more sophisticated range check
            int mobilityScannumberOffset = frame.getMobilityScan(0).getMobilityScanNumber();
            float mobility1 = (float) frame.getMobilityForMobilityScanNumber(
                imsMsMsInfo.getSpectrumNumberRange().lowerEndpoint() - mobilityScannumberOffset);
            float mobility2 = (float) frame.getMobilityForMobilityScanNumber(
                imsMsMsInfo.getSpectrumNumberRange().upperEndpoint() - mobilityScannumberOffset);
            if (Range.singleton(mobility1).span(Range.singleton(mobility2)).contains(mobility)) {
              eligibleMsMsInfos.add(imsMsMsInfo);
            }
          } else {
            // if we don't have a mobility, we can simply add the msms info.
            eligibleMsMsInfos.add(imsMsMsInfo);
          }
        }
      });
    }

    if (eligibleMsMsInfos.isEmpty()) {
      return;
    }

    List<MergedMsMsSpectrum> msmsSpectra = new ArrayList<>();
    for (MsMsInfo info : eligibleMsMsInfos) {
      MergedMsMsSpectrum spectrum = SpectraMerging.getMergedMsMsSpectrumForPASEF(
          (PasefMsMsInfo) info, SpectraMerging.pasefMS2MergeTol, IntensityMergingType.SUMMED,
          ((ModularFeatureList) list).getMemoryMapStorage(),
          lockToFeatureMobilityRange && feature.getMobilityRange() != null
              ? feature.getMobilityRange() : null, minMs2Intensity, minMs2IntensityRel, null);
      if (spectrum != null) {
        msmsSpectra.add(spectrum);
      }
    }

    if (!msmsSpectra.isEmpty()) {
      if (combineTimsMS2) {
        List<Scan> sameCEMerged = SpectraMerging.mergeMsMsSpectra(msmsSpectra,
            SpectraMerging.pasefMS2MergeTol, IntensityMergingType.SUMMED,
            ((ModularFeatureList) list).getMemoryMapStorage());
        feature.setAllMS2FragmentScans(sameCEMerged, true);

      } else {
        feature.setAllMS2FragmentScans((List<Scan>) (List<? extends Scan>) msmsSpectra, true);
      }
    }
  }
}
