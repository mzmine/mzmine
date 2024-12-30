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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsFbmnMgfExportTask extends AbstractTask implements ProcessedItemsCounter {

  // Logger.
  private final Logger logger = Logger.getLogger(getClass().getName());

  //
  private final FeatureList[] featureLists;
  private final File fileName;
  private final FeatureListRowsFilter filter;
  // track number of exported items
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private final IntensityNormalizer normalizer;
  private final SpectralLibraryEntryFactory entryFactory;
  private final @NotNull FragmentScanSelection scanMergeSelect;
  private int currentIndex = 0;

  GnpsFbmnMgfExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists();

    normalizer = parameters.getValue(GnpsFbmnExportAndSubmitParameters.NORMALIZER);
    this.fileName = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).getValue();
    this.filter = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILTER).getValue();
    scanMergeSelect = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.spectraMergeSelect)
        .createFragmentScanSelection(getMemoryMapStorage());

    entryFactory = new SpectralLibraryEntryFactory(true, true, true, false);
    entryFactory.setAddOnlineReactivityFlags(true);
  }

  @Override
  public double getFinishedPercentage() {
    if (featureLists.length == 0) {
      return 1;
    } else {
      return currentIndex / (double) featureLists.length;
    }
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (featureLists.length > 1 && !SiriusExportTask.hasDefaultSubstitutionPattern(fileName)) {
      // error that multiple feature lists are selected and no filename pattern defined
      error("""
          Multiple feature lists (%d) were selected for molecular networking (GNPS) export, /
          but the filename misses the file name pattern "%s" to insert each feature list name.
          Either select a single feature list or use the name pattern.""".formatted(
          featureLists.length, SiriusExportTask.MULTI_NAME_PATTERN));
      return;
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
      final int numRows = rows.size();
      final long numFeatures = rows.stream().count();
      final long numMS2 = rows.stream().filter(FeatureListRow::hasMs2Fragmentation).count();
      final long numFiltered = rows.stream().filter(filter::accept).count();
      // Cancel?
      if (isCanceled()) {
        return;
      }

      currentIndex++;

      // Filename
      final File curFile = SiriusExportTask.getFileForFeatureList(featureList, fileName, "mgf");

      if (!FileAndPathUtil.createDirectory(curFile.getParentFile())) {
        setErrorMessage("Could not create directories for file " + curFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {

        export(featureList, rows, writer);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error during mgf export to " + curFile);
        logger.log(Level.WARNING, "Error during mgf export: " + e.getMessage(), e);
        return;
      }

      // check that nothing has changed during processing
      checkConcurrentModification(featureList, rows, numRows, numFeatures, numMS2, numFiltered);
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void checkConcurrentModification(FeatureList featureList, List<FeatureListRow> rows,
      int numRows, long numFeatures, long numMS2, long numFiltered) {
    final int numRowsEnd = rows.size();
    final long numFeaturesEnd = rows.stream().count();
    final long numMS2End = rows.stream().filter(FeatureListRow::hasMs2Fragmentation).count();
    final long numFilteredEnd = rows.stream().filter(filter::accept).count();

    logger.finer(String.format(
        "flist=%s    MS2=%d    newMS2=%d    features=%d    newF=%d   filtered=%d   fitleredEnd=%d",
        featureList.getName(), numMS2, numMS2End, numFeatures, numFeaturesEnd, numFiltered,
        numFilteredEnd));

    if (numRows != numRowsEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) mgf export old=%d new=%d",
          featureList.getName(), numRows, numRowsEnd));
    }
    if (numFeatures != numFeaturesEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) mgf export old=%d new=%d",
          featureList.getName(), numFeatures, numFeaturesEnd));
    }
    if (numMS2 != numMS2End) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS WITH MS2 during featurelist (%s) mgf export old=%d new=%d",
          featureList.getName(), numMS2, numMS2End));
    }
  }

  private long export(FeatureList featureList, List<FeatureListRow> rows, BufferedWriter writer)
      throws IOException {

    int noMS2Counter = 0;
    // count exported
    for (FeatureListRow row : rows) {
      // do not export if no MSMS
      if (!filter.accept(row)) {
        continue;
      }

      // Get the MS/MS scan
      var selectedScan = scanMergeSelect.getAllFragmentSpectra(row).stream().findFirst();
      if (selectedScan.isEmpty()) {
        noMS2Counter++;
        // with IIMN, filter also accepts feature without MS2
        continue;
      }
      Scan msmsScan = selectedScan.get();

      DataPoint[] dataPoints = null;
      dataPoints = ScanUtils.extractDataPoints(msmsScan, true);

      if (dataPoints == null || dataPoints.length == 0) {
        continue;
      }

      SpectralLibraryEntry entry = entryFactory.createUnknown(null, row, null, msmsScan, dataPoints,
          null, null);

      // requires MS2? or can also be MSn?
//      entry.putIfNotNull(DBEntryField.MS_LEVEL, 2);

      final var mgfEntry = MGFEntryGenerator.createMGFEntry(entry, normalizer);
      if (mgfEntry.numSignals() > 0) {
        writer.write(mgfEntry.spectrum());
        writer.newLine();
        exportedRows.incrementAndGet();
      }
    }

    if (exportedRows.get() == 0) {
      logger.log(Level.WARNING, "No MS/MS scans exported.");
    } else {
      logger.info(
          MessageFormat.format("Total of {0} feature rows (MS/MS mass lists) were exported ({1})",
              exportedRows.get(), featureList.getName()));
    }
    if (noMS2Counter > 0 && filter.requiresMS2()) {
      logger.warning(noMS2Counter + " features had no MS/MS scan after already filtering for MS2");
    }

    return exportedRows.get();
  }

  @Override
  public String getTaskDescription() {
    return "Exporting GNPS of feature list(s) " + Arrays.toString(featureLists) + " to MGF file(s)";
  }

  @Override
  public int getProcessedItems() {
    return exportedRows.get();
  }
}
