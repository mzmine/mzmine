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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import static io.github.mzmine.util.scans.ScanUtils.extractDataPoints;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorResults;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.io.export_scans_modular.ExportScansFeatureTask;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  private final String fileNameWithoutExtension;
  private final SpectralLibraryExportFormats format;
  private final ParameterSet parameters;
  private final Map<DBEntryField, Object> metadataMap;
  private final boolean handleChimerics;
  private final FragmentScanSelection selection;
  private final MsMsQualityChecker msMsQualityChecker;
  private final MsLevelFilter postMergingMsLevelFilter;
  private final IntensityNormalizer normalizer;
  private final SpectralLibraryEntryFactory entryFactory;
  public long totalRows = 0;
  public AtomicInteger finishedRows = new AtomicInteger(0);
  public AtomicInteger exported = new AtomicInteger(0);
  private double minimumPrecursorPurity = 0d;
  private MZTolerance chimericsMainIonMzTol;
  private MZTolerance chimericsIsolationMzTol;
  private ChimericMsOption handleChimericsOption;
  private String description = "Batch exporting spectral library";

  public LibraryBatchGenerationTask(final ParameterSet parameters, final Instant moduleCallDate) {
    super(null, moduleCallDate);
    flists = parameters.getValue(LibraryBatchGenerationParameters.flists).getMatchingFeatureLists();
    format = parameters.getValue(LibraryBatchGenerationParameters.exportFormat);
    this.parameters = parameters;
    String exportFormat = format.getExtension();
    File file = parameters.getValue(LibraryBatchGenerationParameters.file);
    outFile = FileAndPathUtil.getRealFilePath(file, exportFormat);

    fileNameWithoutExtension = FileAndPathUtil.eraseFormat(outFile.getName());

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

    normalizer = parameters.getValue(LibraryBatchGenerationParameters.normalizer);

    // used to extract and merge spectra
    var selectParam = parameters.getParameter(LibraryBatchGenerationParameters.merging)
        .getValueWithParameters();
    selection = selectParam.value()
        .createFragmentScanSelection(getMemoryMapStorage(), selectParam.parameters());

    //
    handleChimerics = parameters.getValue(LibraryBatchGenerationParameters.handleChimerics);
    if (handleChimerics) {
      HandleChimericMsMsParameters param = parameters.getParameter(
          LibraryBatchGenerationParameters.handleChimerics).getEmbeddedParameters();
      minimumPrecursorPurity = param.getValue(HandleChimericMsMsParameters.minimumPrecursorPurity);
      chimericsIsolationMzTol = param.getValue(HandleChimericMsMsParameters.isolationWindow);
      chimericsMainIonMzTol = param.getValue(HandleChimericMsMsParameters.mainMassWindow);
      handleChimericsOption = param.getValue(HandleChimericMsMsParameters.option);
    }

    boolean isAdvanced = parameters.getValue(LibraryBatchGenerationParameters.advanced);
    boolean compactUSI;
    if (isAdvanced) {
      var advanced = parameters.getParameter(LibraryBatchGenerationParameters.advanced)
          .getEmbeddedParameters();
      compactUSI = advanced.getValue(AdvancedLibraryBatchGenerationParameters.compactUSI);
    } else {
      compactUSI = false;
    }

    entryFactory = new SpectralLibraryEntryFactory(compactUSI, false, true, true);
    if (handleChimericsOption == ChimericMsOption.FLAG) {
      entryFactory.setFlagChimerics(true);
    }
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = Arrays.stream(flists).mapToLong(ModularFeatureList::getNumberOfRows).sum();

    try (var writer = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8,
        WriterOptions.REPLACE.toOpenOption())) {
      for (ModularFeatureList flist : flists) {
        description = "Exporting entries for feature list " + flist.getName();
        for (var row : flist.getRows()) {
          processRow(writer, row);
          finishedRows.incrementAndGet();
        }
        flist.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(LibraryBatchGenerationModule.class, parameters,
                getModuleCallDate()));
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

    var featureList = row.getFeatureList();
    // might filter matches for compound name in feature list name
    // only if this option is active
    List<FeatureAnnotation> matches = CompoundAnnotationUtils.streamFeatureAnnotations(row)
        .filter(match -> msMsQualityChecker.matchesName(match, featureList)).toList();

    if (scans.isEmpty() || matches.isEmpty()) {
      return;
    }

    // first entry for the same molecule reflect the most common ion type, usually M+H
    // if multiple compounds match, they are sorted by score descending
    matches = CompoundAnnotationUtils.getBestMatchesPerCompoundName(matches);

    // handle chimerics
    final var chimericMap = handleChimericsAndFilterScansIfSelected(row, scans);

    scans = selectMergeAndFilterScans(scans);
    // export
    exportAllMatches(writer, row, scans, matches, chimericMap);
  }

  /**
   * Selects scans from the list, merges them if active, and filters for MS2 if selected
   *
   * @param scans input list of scans usually from a row
   * @return list of selected scans
   */
  private List<Scan> selectMergeAndFilterScans(List<Scan> scans) {
    // merge spectra, find best spectrum for each MSn node in the tree and each energy
    // filter after merging scans to also generate PSEUDO MS2 from MSn spectra
    scans = selection.getAllFragmentSpectra(scans);
    // TODO maybe remove post merging filter here as selection is done in FragmentScanSelection
    // However, maybe just keep filter as another assurance that only MS2 or MSn is exported?
    // filter scans if selection is only MS2
    if (postMergingMsLevelFilter.isFilter()) {
      scans.removeIf(postMergingMsLevelFilter::notMatch);
    }
    return scans;
  }

  /**
   * Exports all matches
   *
   * @param scans           filtered scans
   * @param filteredMatches filtered annotations, each will be exported
   * @param chimericMap     flags chimeric spectra
   */
  private void exportAllMatches(final BufferedWriter writer, final FeatureListRow row,
      final List<Scan> scans, final List<FeatureAnnotation> filteredMatches,
      final Map<Scan, ChimericPrecursorResults> chimericMap) throws IOException {
    // filtered matches contain one match per compound name sorted by the least complex first
    // M+H better than 2M+H2+2
    for (final FeatureAnnotation match : filteredMatches) {
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

        DataPoint[] dps = explainedSignalsOnly ? score.getAnnotatedDataPoints()
            : extractDataPoints(msmsScan, true);

        var chimeric = chimericMap.getOrDefault(msmsScan, ChimericPrecursorResults.PASSED);

        SpectralLibraryEntry entry = entryFactory.createAnnotated(library.getStorage(), row,
            msmsScan, match, dps, chimeric, score, filteredMatches, metadataMap);

        // specific things that should only happen in library generation - otherwise add to the factory
        final int entryId = exported.incrementAndGet();
        entry.putIfNotNull(DBEntryField.ENTRY_ID, entryId);

        entry.putIfNotNull(DBEntryField.USI,
            ScanUtils.createUSI(entry.getAsString(DBEntryField.DATASET_ID).orElse(null),
                fileNameWithoutExtension, String.valueOf(entryId)));

        ExportScansFeatureTask.exportEntry(writer, entry, format, normalizer);
      }
    }
  }


  /**
   * @param scans might be filtered if the chimeric handling is set to SKIP
   * @return a map that flags spectra as chimeric or passed - or an empty map if handeChimerics is
   * off
   */
  @NotNull
  private Map<Scan, ChimericPrecursorResults> handleChimericsAndFilterScansIfSelected(
      final FeatureListRow row, final List<Scan> scans) {
    return ExportScansFeatureTask.handleChimericsAndFilterScansIfSelected(chimericsIsolationMzTol,
        chimericsMainIonMzTol, handleChimerics, handleChimericsOption, minimumPrecursorPurity, row,
        scans);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

}
