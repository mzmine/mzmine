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

package io.github.mzmine.modules.dataprocessing.process_fragmentsanalysis;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.CommonFragmentsType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
class FragmentsAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(FragmentsAnalysisTask.class.getName());

  private final List<FeatureList> featureLists;
  private final File outFile;
  private final boolean useMassList;
  private final MZTolerance tolerance;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureLists data source is featureLists
   * @param parameters   user parameters
   */
  public FragmentsAnalysisTask(MZmineProject project, List<FeatureList> featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    // important to track progress with totalItems and finishedItems
    totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    // Get parameter values for easier use
    outFile = parameters.getValue(FragmentsAnalysisParameters.outFile);
    var scanDataType = parameters.getValue(FragmentsAnalysisParameters.scanDataType);
    useMassList = scanDataType == ScanDataType.MASS_LIST;
    tolerance = parameters.getValue(FragmentsAnalysisParameters.tolerance);
  }

  @Override
  protected void process() {
    // write all spectra to mgf and also put them into
    List<GroupedFragmentScans> groupedScans = collectAndWriteSpectraToMgf();
    logger.info("mgf export finished - now starting to analyze the grouped scans");

    // TODO find signals that are common in all MS1 scans

    // flatMap is used to unwrap a List into its individual elements in a stream
    streamMs1Scans(groupedScans).forEach(ms1 -> {
      // this streams through all MS1 scans in all groupings... just in case you want to create a histogram or similar things
      // for example
    });

    // this could be used to bin the MS1 signal frequency in mz range x-z
    var ms1MzFrequency = new DataHistogramBinner(0.001, 25, 2000);
    streamMs1Scans(groupedScans).forEach(ms1 -> {
      DataPoint[] data = ScanUtils.extractDataPoints(ms1, useMassList);
      for (DataPoint dp : data) {
        ms1MzFrequency.addValue(dp.getMZ());
      }
    });

    int frequencyOfMz200 = ms1MzFrequency.getBinFrequency(200);

  }

  private static @NotNull Stream<Scan> streamMs1Scans(
      final List<GroupedFragmentScans> groupedScans) {
    return groupedScans.stream().map(GroupedFragmentScans::ms1Scans).flatMap(Collection::stream);
  }

  private List<GroupedFragmentScans> collectAndWriteSpectraToMgf() {
    logger.info("mgf export of grouped scans started");

    List<GroupedFragmentScans> groupingResults = new ArrayList<>();
    // Open file
    try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),
        StandardCharsets.UTF_8)) {
      logger.fine(() -> String.format("Exporting GDebunk mgf for feature list: to file %s",
          outFile.getAbsolutePath()));
      for (FeatureList featureList : featureLists) {
        for (var row : featureList.getRows()) {
          GroupedFragmentScans result = processRow(writer, row);
          if (!result.ms2Scans().isEmpty()) {
            groupingResults.add(result);
          }
        }
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outFile + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing GDebunk mgf format to file: %s. Message: %s",
              outFile.getAbsolutePath(), e.getMessage()), e);
      // on error return empty list
      return List.of();
    }
    return groupingResults;
  }

  private GroupedFragmentScans processRow(BufferedWriter writer, FeatureListRow row)
      throws IOException {
    // collect all MS1 and MS2 for this row
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> ms2Scans = new ArrayList<>();

    for (final ModularFeature feature : row.getFeatures()) {
      try {
        List<Scan> exportedScans = processFeature(writer, row, feature);
        for (Scan scan : exportedScans) {
          if (scan.getMSLevel() == 1) {
            ms1Scans.add(scan);
          }
          if (scan.getMSLevel() == 2) {
            ms2Scans.add(scan);
          }
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    }

    // Count unique common fragments for this row and log it
    int commonFragmentsCount = countUniqueFragmentsBetweenMs1AndMs2(ms1Scans, ms2Scans, tolerance);
    logger.info(String.format("Row %d: Total number of unique common fragments between MS1 and MS2 scans: %d",
            row.getID(), commonFragmentsCount));

    row.set(CommonFragmentsType.class, commonFragmentsCount);

    return new GroupedFragmentScans(row, ms1Scans, ms2Scans);
  }

  /**
   * For each feature (individual sample)
   *
   * @return all exported scans
   */
  private List<Scan> processFeature(final BufferedWriter writer, final FeatureListRow row,
      final Feature feature) throws IOException {
    // skip if there are no MS2
    List<Scan> fragmentScans = feature.getAllMS2FragmentScans();
    if (fragmentScans.isEmpty()) {
      return List.of(); // return empty list
    }

    RawDataFile raw = feature.getRawDataFile();
    String rawFileName = raw.getFileName();

    // collect all scans to export
    List<Scan> scansToExport = new ArrayList<>();

    Scan bestMs1 = feature.getRepresentativeScan();
    if (bestMs1 != null) {
      scansToExport.add(bestMs1);
    }

    for (Scan ms2 : fragmentScans) {
      if (ms2.getMSLevel() != 2) {
        continue; // skip MSn
      }
      if (ms2 != null) {
        scansToExport.add(ms2);
      }

      Scan previousScan = ScanUtils.findPrecursorScan(ms2);
      if (previousScan != null) {
        scansToExport.add(previousScan);
      }
      Scan nextScan = ScanUtils.findSucceedingPrecursorScan(ms2);
      if (nextScan != null) {
        scansToExport.add(nextScan);
      }
    }

    for (final Scan scan : scansToExport) {
      exportScanToMgf(writer, row, rawFileName, scan);
    }

    return scansToExport;
  }

  private void exportScanToMgf(BufferedWriter writer, FeatureListRow row, final String rawFileName,
      Scan scan) throws IOException {
    String id = String.format("%s_id%d", rawFileName, row.getID());
    String usi = String.format("%s:%d", rawFileName, scan.getScanNumber());

    FeatureAnnotation annotation = CompoundAnnotationUtils.streamFeatureAnnotations(row).findFirst()
        .orElse(null);
    DataPoint[] data = ScanUtils.extractDataPoints(scan, useMassList);
    // create entry
    var entry = SpectralLibraryEntry.create(row, null, scan, annotation, data);
    // add additional information
    entry.putIfNotNull(DBEntryField.ENTRY_ID, id);
    entry.putIfNotNull(DBEntryField.USI, usi);
    // export
    final String mgfEntry = MGFEntryGenerator.createMGFEntry(entry);
    writer.write(mgfEntry);
    writer.newLine();
  }

  @Override
  public String getTaskDescription() {
    return STR."Fragments analysis task runs on \{featureLists}";
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return featureLists;
  }

  // TODO: Sanitize the spectra by removing everything above the precursor - tolerance? And more?
  private int countUniqueFragmentsBetweenMs1AndMs2(List<Scan> ms1Scans, List<Scan> ms2Scans, MZTolerance tolerance) {
    Set<Double> uniqueMs1 = collectUniqueFragments(ms1Scans, tolerance);
    Set<Double> uniqueMs2 = collectUniqueFragments(ms2Scans, tolerance);

    int uniqueCount = countUniquePairs(uniqueMs1, uniqueMs2, tolerance);

    return uniqueCount;
  }

  private Set<Double> collectUniqueFragments(List<Scan> scans, MZTolerance tolerance) {
    Set<Double> uniqueFragments = new HashSet<>();

    for (Scan scan : scans) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(scan, useMassList);
      for (DataPoint dp : dataPoints) {
        double mz = dp.getMZ();
        boolean isUnique = true;

        // Check against existing uniqueFragments
        for (double uniqueMz : uniqueFragments) {
          if (tolerance.checkWithinTolerance(mz, uniqueMz)) {
            isUnique = false;
            break;
          }
        }

        if (isUnique) {
          uniqueFragments.add(mz);
        }
      }
    }

    return uniqueFragments;
  }

  private int countUniquePairs(Set<Double> uniqueMs1, Set<Double> uniqueMs2, MZTolerance tolerance) {
    int uniqueCount = 0;

    for (double mz1 : uniqueMs1) {
      for (double mz2 : uniqueMs2) {
        if (tolerance.checkWithinTolerance(mz1, mz2)) {
          uniqueCount++;
          break; // Move to next mz1
        }
      }
    }

    return uniqueCount;
  }

}
