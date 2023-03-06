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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import static io.github.mzmine.util.scans.ScanUtils.extractDataPoints;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MSPEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MZmineJsonGenerator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSelection.IncludeInputSpectra;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibraryBatchGenerationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LibraryBatchGenerationTask.class.getName());
  private final SpectralLibrary library;
  private final ModularFeatureList[] flists;
  private final File outFile;
  private final SpectralLibraryExportFormats format;
  private final Map<DBEntryField, Object> metadataMap;
  private final boolean handleChimerics;
  private final FragmentScanSelection selection;
  private final MsMsQualityChecker msMsQualityChecker;
  private final MZTolerance mzTolMerging;
  private final boolean enableMsnMerge;
  private final MsLevelFilter postMergingMsLevelFilter;
  private double allowedOtherSignalSum = 0d;
  private MZTolerance mzTolChimericsMainIon;
  private MZTolerance mzTolChimericsIsolation;
  private ChimericMsOption handleChimericsOption;
  public long totalRows = 0;
  public AtomicInteger finishedRows = new AtomicInteger(0);
  public AtomicInteger exported = new AtomicInteger(0);
  private String description = "Batch exporting spectral library";

  public LibraryBatchGenerationTask(final ParameterSet parameters, final Instant moduleCallDate) {
    super(null, moduleCallDate);
    flists = parameters.getValue(LibraryBatchGenerationParameters.flists).getMatchingFeatureLists();
    format = parameters.getValue(LibraryBatchGenerationParameters.exportFormat);
    String exportFormat = format.getExtension();
    File file = parameters.getValue(LibraryBatchGenerationParameters.file);
    outFile = FileAndPathUtil.getRealFilePath(file, exportFormat);

    library = new SpectralLibrary(MemoryMapStorage.forMassList(), outFile.getName() + "_batch",
        outFile);
    // metadata as a map
    LibraryBatchMetadataParameters meta = parameters.getParameter(
        LibraryBatchGenerationParameters.metadata).getEmbeddedParameters();
    metadataMap = meta.asMap();

    msMsQualityChecker = parameters.getParameter(LibraryBatchGenerationParameters.quality)
        .getEmbeddedParameters().toQualityChecker();

    postMergingMsLevelFilter = parameters.getValue(
        LibraryBatchGenerationParameters.postMergingMsLevelFilter);

    enableMsnMerge = parameters.getValue(LibraryBatchGenerationParameters.mergeMzTolerance);
    mzTolMerging = parameters.getEmbeddedParameterValue(
        LibraryBatchGenerationParameters.mergeMzTolerance);
    //
    handleChimerics = parameters.getValue(LibraryBatchGenerationParameters.handleChimerics);
    if (handleChimerics) {
      HandleChimericMsMsParameters param = parameters.getParameter(
          LibraryBatchGenerationParameters.handleChimerics).getEmbeddedParameters();
      allowedOtherSignalSum = param.getValue(HandleChimericMsMsParameters.allowedOtherSignals);
      mzTolChimericsIsolation = param.getValue(HandleChimericMsMsParameters.isolationWindow);
      mzTolChimericsMainIon = param.getValue(HandleChimericMsMsParameters.mainMassWindow);
      handleChimericsOption = param.getValue(HandleChimericMsMsParameters.option);
    }

    // used to extract and merge spectra
    selection = new FragmentScanSelection(mzTolMerging, true,
        IncludeInputSpectra.HIGHEST_TIC_PER_ENERGY, IntensityMergingType.MAXIMUM,
        postMergingMsLevelFilter);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = Arrays.stream(flists).mapToLong(ModularFeatureList::getNumberOfRows).sum();

    try (var writer = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
      for (ModularFeatureList flist : flists) {
        description = "Exporting entries for feature list " + flist.getName();
        for (var row : flist.getRows()) {
          processRow(writer, row);
          finishedRows.incrementAndGet();
        }
      }
      //
      logger.info(String.format("Exported %d new library entries to file %s", exported.get(),
          outFile.getAbsolutePath()));
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outFile + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing library file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
      return;
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not export library to " + outFile + " because of internal exception:"
          + e.getMessage());
      logger.log(Level.WARNING,
          String.format("Error writing library file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processRow(final BufferedWriter writer, final FeatureListRow row)
      throws IOException {
    List<Scan> scans = row.getAllFragmentScans();
    List<CompoundDBAnnotation> matches = row.getCompoundAnnotations();
    if (scans.isEmpty() || matches.isEmpty()) {
      return;
    }

    // first entry for the same molecule reflect the most common ion type, usually M+H
    var match = matches.get(0);

    if (!msMsQualityChecker.matchesName(match, row.getFeatureList())) {
      return;
    }

    // handle chimerics
    final var chimericMap = handleChimericsAndFilterScansIfSelected(row, scans);

    if (enableMsnMerge) {
      // merge spectra, find best spectrum for each MSn node in the tree and each energy
      // filter after merging scans to also generate PSEUDO MS2 from MSn spectra
      scans = selection.getAllFragmentSpectra(scans);
    } else {
      // filter scans if selection is only MS2
      if (postMergingMsLevelFilter.isFilter()) {
        scans.removeIf(postMergingMsLevelFilter::notMatch);
      }
    }
    // cache all formulas
    IMolecularFormula formula = FormulaUtils.getIonizedFormula(match);
    FormulaWithExactMz[] sortedFormulas = FormulaUtils.getAllFormulas(formula, 1, 15);

    boolean explainedSignalsOnly = msMsQualityChecker.exportExplainedSignalsOnly();
    // filter matches
    for (final Scan msmsScan : scans) {
      final MSMSScore score = msMsQualityChecker.match(msmsScan, match, sortedFormulas);
      if (score.isFailed(false)) {
        continue;
      }

      DataPoint[] dps =
          explainedSignalsOnly ? score.getAnnotatedDataPoints() : extractDataPoints(msmsScan, true);

      SpectralLibraryEntry entry = createEntry(row, match, chimericMap, msmsScan, score, dps);
      exportEntry(writer, entry);
      exported.incrementAndGet();
    }
  }

  @NotNull
  private SpectralLibraryEntry createEntry(final FeatureListRow row,
      final CompoundDBAnnotation match, final Map<Scan, ChimericPrecursorResult> chimericMap,
      final Scan msmsScan, final MSMSScore score, final DataPoint[] dps) {
    // add instrument type etc by parameter
    SpectralLibraryEntry entry = SpectralLibraryEntry.create(library.getStorage(), msmsScan, match,
        dps);
    entry.putAll(metadataMap);

    // score might be successful without having a formula - so check if we actually have scores
    if (score.explainedSignals() > 0) {
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_INTENSITY, score.explainedIntensity());
      entry.putIfNotNull(DBEntryField.QUALITY_EXPLAINED_SIGNALS, score.explainedSignals());
    }
    if (ChimericMsOption.FLAG.equals(handleChimericsOption)) {
      // default is passed
      ChimericPrecursorResult chimeric = chimericMap.getOrDefault(msmsScan,
          ChimericPrecursorResult.PASSED);
      entry.putIfNotNull(DBEntryField.QUALITY_CHIMERIC, chimeric);
      if (ChimericPrecursorResult.CHIMERIC.equals(chimeric)) {
        entry.putIfNotNull(DBEntryField.NAME,
            entry.getField(DBEntryField.NAME).orElse("") + " (Chimeric precursor selection)");
      }
    }
    // add file info
    int scanNumber = msmsScan.getScanNumber();
    final String fileUSI = Path.of(
        Objects.requireNonNullElse(msmsScan.getDataFile().getAbsolutePath(),
            msmsScan.getDataFile().getName())).getFileName().toString() + ":" + scanNumber;

    entry.putIfNotNull(DBEntryField.SCAN_NUMBER, scanNumber);
    entry.getField(DBEntryField.DATASET_ID).ifPresent(
        dataID -> entry.putIfNotNull(DBEntryField.USI, "mzspec:" + dataID + ":" + fileUSI));

    // add experimental data
    if (entry.getField(DBEntryField.RT).isEmpty()) {
      entry.putIfNotNull(DBEntryField.RT, row.getAverageRT());
    }
    if (entry.getField(DBEntryField.CCS).isEmpty()) {
      entry.putIfNotNull(DBEntryField.CCS, row.getAverageCCS());
    }
    return entry;
  }


  private Map<Scan, ChimericPrecursorResult> checkChimericPrecursorIsolation(
      final FeatureListRow row, final List<Scan> scans) {
    // all data files from fragment scans to score if is chimeric
    Map<Scan, ChimericPrecursorResult> chimericMap = new HashMap<>();
    for (Scan scan : scans) {
      chimericMap.computeIfAbsent(scan, key -> scoreChimericIsolation(row, scan));
    }
    return chimericMap;
  }

  /**
   * @param scans might be filtered if the chimeric handling is set to SKIP
   * @return a map that flags spectra as chimeric or passed - or an empty map if handeChimerics is
   * off
   */
  private Map<Scan, ChimericPrecursorResult> handleChimericsAndFilterScansIfSelected(
      final FeatureListRow row, final List<Scan> scans) {
    if (handleChimerics) {
      var chimericMap = checkChimericPrecursorIsolation(row, scans);
      if (ChimericMsOption.SKIP.equals(handleChimericsOption)) {
        scans.removeIf(scan -> ChimericPrecursorResult.PASSED != chimericMap.get(scan));
      }
      return chimericMap;
    }
    return Map.of();
  }

  private ChimericPrecursorResult scoreChimericIsolation(final FeatureListRow row,
      final Scan scan) {
    RawDataFile raw = scan.getDataFile();
    Feature feature = row.getFeature(raw);
    if (feature == null) {
      return ChimericPrecursorResult.PASSED;
    }

    // retrieve preceding ms1 scan
    final Scan ms1;
    if (scan instanceof MergedMsMsSpectrum msms) {
      ms1 = ScanUtils.findPrecursorScanForMerged(msms, SpectraMerging.defaultMs1MergeTol);
    } else {
      ms1 = ScanUtils.findPrecursorScan(scan);
    }

    if (ms1 == null) {
      // maybe there was no MS1 before that?
      logger.finest(() -> String.format("Could not find MS1 before this scan: %s", scan));
      return ChimericPrecursorResult.PASSED;
    }

    MassList massList = ms1.getMassList();
    if (massList == null) {
      throw new MissingMassListException(feature.getRepresentativeScan());
    }

    // check for signals in isolation range
    return ChimericPrecursorResult.check(massList, row.getAverageMZ(), mzTolChimericsMainIon,
        mzTolChimericsIsolation, allowedOtherSignalSum);
  }

  private void exportEntry(final BufferedWriter writer, final SpectralLibraryEntry entry)
      throws IOException {
    String stringEntry = switch (format) {
      case msp -> MSPEntryGenerator.createMSPEntry(entry);
      case json -> MZmineJsonGenerator.generateJSON(entry);
      case mgf -> MGFEntryGenerator.createMGFEntry(entry);
    };
    writer.append(stringEntry).append("\n");
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

}
