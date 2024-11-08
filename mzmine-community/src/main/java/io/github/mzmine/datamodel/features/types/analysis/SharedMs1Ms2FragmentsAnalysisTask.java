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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    // TODO
    // tolerance = ...
  }

  @Override
  protected void process() {
    // write all spectra to mgf and also put them into
    List<GroupedFragmentScans> groupedScans = collectAndWriteSpectraToMgf();
    logger.info("mgf export finished - now starting to analyze the grouped scans");

    // TODO find signals that are common in all MS1 scans

    // flatMap is used to unwrap a List into its individual elements in a stream
    groupedScans.stream().map(GroupedFragmentScans::ms1Scans).flatMap(Collection::stream)
        .forEach(ms1 -> {
          // this streams through all MS1 scans in all groupings... just in case you want to create a histogram or similar things
          // for example
        });

    // this could be used to bin the MS1 signal frequency in mz range x-z
    var ms1MzFrequency = new DataHistogramBinner(0.001, 25, 2000);
    groupedScans.stream().map(GroupedFragmentScans::ms1Scans).flatMap(Collection::stream)
        .forEach(ms1 -> {
          DataPoint[] data = ScanUtils.extractDataPoints(ms1, useMassList);
          for (DataPoint dp : data) {
            ms1MzFrequency.addValue(dp.getMZ());
          }
        });

    int frequencyOfMz200 = ms1MzFrequency.getBinFrequency(200);

    // TODO count signals in MS1 scans that are filtered out due to
    //  1. common contamination
    //  2. in source fragment found in corresponding MS2

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
    scansToExport.add(bestMs1);

    for (Scan ms2 : fragmentScans) {
      if (ms2.getMSLevel() != 2) {
        continue; // skip MSn
      }
      scansToExport.add(ms2);

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

// TODO
//    private int countCommonFragments(Scan ms1, Scan ms2, double tolerance) {
//        DoubleStream ms1Fragments = DoubleStream.of(ScanUtils.getMzValues(ms1));
//        DoubleStream ms2Fragments = DoubleStream.of(ScanUtils.getMzValues(ms2));
//
//        List<Double> ms1FragmentList = ms1Fragments.boxed().collect(Collectors.toList());
//        List<Double> ms2FragmentList = ms2Fragments.boxed().collect(Collectors.toList());
//
//        int commonFragments = 0;
//        for (Double fragment : ms1FragmentList) {
//            for (Double ms2Fragment : ms2FragmentList) {
//                if (Math.abs(fragment - ms2Fragment) <= tolerance) {
//                    commonFragments++;
//                    break; // move to next fragment in ms1FragmentList
//                }
//            }
//        }
//        return commonFragments;
//    }
}
