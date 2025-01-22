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

package io.github.mzmine.modules.io.export_scans_modular;

import static io.github.mzmine.util.scans.ScanUtils.extractDataPoints;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorChecker;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorFlag;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.ChimericPrecursorResults;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MSPEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MZmineJsonGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.WrongParameterConfigException;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportScansFeatureTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(ExportScansFeatureTask.class.getName());
  private final @NotNull FeatureList[] featureLists;
  private final SpectralLibraryExportFormats format;
  private final File outFile;
  private final Map<DBEntryField, Object> metadataMap;
  private final IntensityNormalizer normalizer;
  private final @NotNull FragmentScanSelection selection;
  private final Boolean handleChimerics;
  private final SpectralLibraryEntryFactory entryFactory;
  private final int minFragmentSignals;
  private final boolean exportMs1;
  private final boolean separateMs1File;
  private final Ms1ScanSelection ms1Selection;
  private final boolean ms1RequiresFragmentScan;
  private final boolean skipAnnotatedFeatures;
  private double minimumPrecursorPurity;
  private MZTolerance chimericsIsolationMzTol;
  private MZTolerance chimericsMainIonMzTol;
  private ChimericMsOption handleChimericsOption;

  // writers for MSn >=2 and for MS1 - MS1 might be the same as MSn
  private String ms1FileNameWithoutExtension;
  private String msnFileNameWithoutExtension;
  private BufferedWriter msnWriter;
  private BufferedWriter ms1Writer;

  // status
  public AtomicInteger exported = new AtomicInteger(0);
  private String description = "";
  private @Nullable ProjectMetadataToLibraryEntryMapper projectMetadataMapper = null;

  protected ExportScansFeatureTask(final @Nullable MemoryMapStorage storage,
      final @NotNull Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      final @NotNull FeatureList[] featureLists) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    format = parameters.getValue(ExportScansFeatureMainParameters.exportFormat);
    String exportFormat = format.getExtension();
    File file = parameters.getValue(ExportScansFeatureMainParameters.file);
    outFile = FileAndPathUtil.getRealFilePath(file, exportFormat);

    // metadata as a map
    LibraryBatchMetadataParameters meta = parameters.getParameter(
        ExportScansFeatureMainParameters.metadata).getEmbeddedParameters();
    metadataMap = meta.asMap();

    skipAnnotatedFeatures = parameters.getValue(
        ExportScansFeatureMainParameters.skipAnnotatedFeatures);
    normalizer = parameters.getValue(ExportScansFeatureMainParameters.normalizer);

    exportMs1 = parameters.getValue(ExportScansFeatureMainParameters.exportMs1);
    var ms1Params = parameters.getParameter(ExportScansFeatureMainParameters.exportMs1)
        .getEmbeddedParameters();
    if (exportMs1) {
      separateMs1File = ms1Params.getValue(ExportMs1ScansFeatureParameters.separateMs1File);
      ms1Selection = ms1Params.getValue(ExportMs1ScansFeatureParameters.ms1Selection);
      ms1RequiresFragmentScan = ms1Params.getValue(
          ExportMs1ScansFeatureParameters.ms1RequiresFragmentScan);
    } else {
      ms1RequiresFragmentScan = true;
      separateMs1File = false;
      ms1Selection = Ms1ScanSelection.CORRELATED;
    }

    // used to extract and merge spectra
    var ms2Params = parameters.getParameter(ExportScansFeatureMainParameters.exportFragmentScans)
        .getEmbeddedParameters();
    selection = ms2Params.getParameter(ExportFragmentScansFeatureParameters.merging)
        .createFragmentScanSelection(getMemoryMapStorage());

    minFragmentSignals = ms2Params.getValue(ExportFragmentScansFeatureParameters.minSignals);

    //
    handleChimerics = ms2Params.getValue(ExportFragmentScansFeatureParameters.handleChimerics);
    if (handleChimerics) {
      HandleChimericMsMsParameters param = ms2Params.getParameter(
          ExportFragmentScansFeatureParameters.handleChimerics).getEmbeddedParameters();
      minimumPrecursorPurity = param.getValue(HandleChimericMsMsParameters.minimumPrecursorPurity);
      chimericsIsolationMzTol = param.getValue(HandleChimericMsMsParameters.isolationWindow);
      chimericsMainIonMzTol = param.getValue(HandleChimericMsMsParameters.mainMassWindow);
      handleChimericsOption = param.getValue(HandleChimericMsMsParameters.option);
    }

    boolean isAdvanced = parameters.getValue(ExportScansFeatureMainParameters.advanced);
    boolean compactUSI;
    if (isAdvanced) {
      var advanced = parameters.getParameter(ExportScansFeatureMainParameters.advanced)
          .getEmbeddedParameters();
      compactUSI = advanced.getValue(AdvancedExportScansFeatureParameters.compactUSI);
    } else {
      compactUSI = false;
    }

    entryFactory = new SpectralLibraryEntryFactory(compactUSI, false, true, true);
    if (handleChimericsOption == ChimericMsOption.FLAG) {
      entryFactory.setFlagChimerics(true);
    }

    if (featureLists.length > 1) {
      this.description = "Exporting scans for %d feature lists".formatted(featureLists.length);
    } else if (featureLists.length == 1) {
      description = "Exporting scans for feature list " + featureLists[0].getName();
    }
  }

  @Override
  protected void process() {
    try {
      // mapper for sample wide project metadata
      initProjectMetadataMapper();
    } catch (WrongParameterConfigException e) {
      error(e.getMessage(), e);
      return;
    }

    totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();

    final boolean separateFiles = outFile.getName().contains(SiriusExportTask.MULTI_NAME_PATTERN);
    if (separateFiles) {
      for (final FeatureList featureList : featureLists) {
        // export each in a separate file
        var featureListOutFile = SiriusExportTask.getFileForFeatureList(featureList, outFile,
            SiriusExportTask.MULTI_NAME_PATTERN, format.getExtension());
        exportFeatureLists(new FeatureList[]{featureList}, featureListOutFile);
      }
    } else {
      // export all in a single file
      exportFeatureLists(featureLists, outFile);
    }
  }

  private void initProjectMetadataMapper() throws WrongParameterConfigException {
    var mapperParam = parameters.getParameter(
        ExportScansFeatureMainParameters.projectMetadataMapper);
    final boolean useProjectMetadata = mapperParam.getValue();
    if (!useProjectMetadata) {
      return;
    }
    projectMetadataMapper = mapperParam.getEmbeddedParameters().createMapper();
  }

  protected void exportFeatureLists(final FeatureList[] featureLists, final File outFile) {
    try {
      msnWriter = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8,
          WriterOptions.REPLACE.toOpenOption());
      msnFileNameWithoutExtension = FileAndPathUtil.eraseFormat(outFile.getName());
      if (exportMs1 && separateMs1File) {
        final File ms1File = FileAndPathUtil.getRealFilePathWithSuffix(outFile, "_ms1");
        ms1Writer = Files.newBufferedWriter(ms1File.toPath(), StandardCharsets.UTF_8,
            WriterOptions.REPLACE.toOpenOption());
        ms1FileNameWithoutExtension = FileAndPathUtil.eraseFormat(ms1File.getName());
      } else {
        ms1Writer = msnWriter;
        ms1FileNameWithoutExtension = msnFileNameWithoutExtension;
      }

      for (FeatureList flist : featureLists) {
        description = "Exporting scan entries for feature list " + flist.getName();
        for (var row : flist.getRows()) {
          if (!checkPreConditions(row)) {
            finishedItems.incrementAndGet();
            continue;
          }
          exportRow(row);
          finishedItems.incrementAndGet();
        }
        flist.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(LibraryBatchGenerationModule.class, parameters,
                getModuleCallDate()));
      }
      //
      logger.info(String.format("Exported %d new library entries to file %s", exported.get(),
          outFile.getAbsolutePath()));
    } catch (IOException e) {
      error("Could not open file " + outFile + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing scans file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
    } catch (Throwable e) {
      error("Could not export scans file to " + outFile + " because of internal exception:"
            + e.getMessage());
      logger.log(Level.WARNING,
          String.format("Error writing scans file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
    } finally {
      // important close writers after catching any Exception
      closeWriters();
    }
  }

  private void closeWriters() {
    try {
      msnWriter.close();
    } catch (Throwable e) {
    }
    try {
      if (ms1Writer != msnWriter) { // uses either the same or a different writer
        ms1Writer.close();
      }
    } catch (Throwable e) {
    }
  }

  private void exportRow(final FeatureListRow row) throws IOException {
    // get all fragment scans as entries to decide whether to export MS1 or not
    final List<SpectralLibraryEntry> fragmentScans = prepareFragmentScans(row);

    if (ms1RequiresFragmentScan && fragmentScans.isEmpty()) {
      return;
    }

    final List<SpectralLibraryEntry> ms1Scans =
        exportMs1 ? prepareMs1Scans(row) : new ArrayList<>();

    // optional filtering of entries
    filterEntries(row, ms1Scans, fragmentScans);

    // fragment scans may be empty now
    if (ms1RequiresFragmentScan && fragmentScans.isEmpty()) {
      return;
    }

    // export MS1 scans
    for (final SpectralLibraryEntry ms1 : ms1Scans) {
      exportScan(ms1Writer, ms1FileNameWithoutExtension, ms1);
    }

    // export MS2 scans
    for (final SpectralLibraryEntry msn : fragmentScans) {
      exportScan(msnWriter, msnFileNameWithoutExtension, msn);
    }
  }

  /**
   * Filter out rows based on conditions like charge state
   *
   * @return true if row is accepted and should be processed
   */
  protected boolean checkPreConditions(final FeatureListRow row) {
    // option to overwrite this method to control which row is processed
    if (skipAnnotatedFeatures && CompoundAnnotationUtils.streamFeatureAnnotations(row).findFirst()
        .isPresent()) {
      return false;
    }
    return true;
  }

  /**
   * Controls filtering of ms1Scans and fragmentScans before export
   *
   * @param row           source of scans
   * @param ms1Scans      modifiable list of ms1 scans (MS1 and correlated if selected)
   * @param fragmentScans modifiable list of MS2 and MSn scans
   */
  protected void filterEntries(final FeatureListRow row, final List<SpectralLibraryEntry> ms1Scans,
      final List<SpectralLibraryEntry> fragmentScans) {
    // option to overwrite to control filtering of ms1Scans and fragmentScans before export
  }

  /**
   * Correlated and MS1 spectrum if selected
   *
   * @return modifiable list of spectral library entries for MS1
   */
  private List<SpectralLibraryEntry> prepareMs1Scans(final FeatureListRow row) {
    if (!exportMs1) {
      return new ArrayList<>();
    }

    List<SpectralLibraryEntry> entries = new ArrayList<>();
    if (ms1Selection.includesCorrelated()) {
      // isotope pattern + adducts etc
      var correlated = SiriusExportTask.generateCorrelationSpectrum(entryFactory,
          MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, row, null, metadataMap);
      if (correlated != null) {
        entries.add(correlated);
      }
    }
    if (ms1Selection.includesMs1()) {
      Feature bestFeature = row.getBestFeature();
      Scan ms1 = bestFeature.getRepresentativeScan();
      SpectralLibraryEntry ms1Entry = spectrumToEntry(ms1, row, bestFeature);
      if (ms1Entry != null) {
        entries.add(ms1Entry);
      }
    }
    return entries;
  }

  private void exportScan(final BufferedWriter writer, final String fileNameWithoutExtension,
      final SpectralLibraryEntry entry) throws IOException {
    // specific things that should only happen in library generation - otherwise add to the factory
    final int entryId = exported.incrementAndGet();
    entry.putIfNotNull(DBEntryField.ENTRY_ID, entryId);

    entry.putIfNotNull(DBEntryField.USI,
        ScanUtils.createUSI(entry.getAsString(DBEntryField.DATASET_ID).orElse(null),
            fileNameWithoutExtension, String.valueOf(entryId)));

    // export
    ExportScansFeatureTask.exportEntry(writer, entry, format, normalizer);
  }

  public SpectralLibraryEntry spectrumToEntry(MassSpectrum spectrum,
      final @Nullable FeatureListRow row, final @Nullable Feature f) {
    final DataPoint[] data = ScanUtils.extractDataPoints(spectrum, true);

    // create unknown to not interfere with annotation by sirius by adding to much info
    final SpectralLibraryEntry entry = entryFactory.createUnknown(null, row, f, spectrum, data,
        null, metadataMap);
    // below here are only SIRIUS specific fields added or overwritten.
    // all default behavior should go into {@link SpectralLibraryEntryFactory}

    return entry;
  }

  /**
   * @return modifiable list of MS2 and MSn entries
   */
  @NotNull
  private List<SpectralLibraryEntry> prepareFragmentScans(final FeatureListRow row) {
    List<Scan> scans = row.getAllFragmentScans();

    if (scans.isEmpty()) {
      return List.of();
    }

    // handle chimerics
    final var chimericMap = handleChimericsAndFilterScansIfSelected(row, scans);

    scans = selectMergeAndFilterScans(scans);

    List<SpectralLibraryEntry> entries = new ArrayList<>();
    for (final Scan scan : scans) {
      DataPoint[] dps = extractDataPoints(scan, true);
      if (dps.length < minFragmentSignals) {
        continue;
      }

      var chimeric = chimericMap.getOrDefault(scan, ChimericPrecursorResults.PASSED);

      SpectralLibraryEntry entry = entryFactory.createUnknown(getMemoryMapStorage(), row, null,
          scan, dps, chimeric, metadataMap);

      if (projectMetadataMapper != null) {
        projectMetadataMapper.addMetadataToEntry(entry, scan);
      }

      entries.add(entry);
    }
    return entries;
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
    return scans;
  }

  public static void exportEntry(final @NotNull BufferedWriter writer,
      final @NotNull SpectralLibraryEntry entry, final @NotNull SpectralLibraryExportFormats format,
      final @NotNull IntensityNormalizer normalizer) throws IOException {
    // TODO maybe skip empty spectra. After formatting the number of signals may be smaller than before
    // if intensity is 0 after formatting
    String stringEntry = switch (format) {
      case msp -> MSPEntryGenerator.createMSPEntry(entry, normalizer);
      case json_mzmine -> MZmineJsonGenerator.generateJSON(entry, normalizer);
      case mgf -> MGFEntryGenerator.createMGFEntry(entry, normalizer).spectrum();
    };
    writer.append(stringEntry).append("\n");
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

  /**
   * @param scans might be filtered if the chimeric handling is set to SKIP
   * @return a map that flags spectra as chimeric or passed - or an empty map if handeChimerics is
   * off
   */
  @NotNull
  public static Map<Scan, ChimericPrecursorResults> handleChimericsAndFilterScansIfSelected(
      final MZTolerance chimericsIsolationMzTol, final MZTolerance chimericsMainIonMzTol,
      final boolean handleChimerics, final ChimericMsOption handleChimericsOption,
      final double minimumPrecursorPurity, final FeatureListRow row, final List<Scan> scans) {
    if (handleChimerics) {
      var chimericMap = ChimericPrecursorChecker.checkChimericPrecursorIsolation(row.getAverageMZ(),
          scans, chimericsMainIonMzTol, chimericsIsolationMzTol, minimumPrecursorPurity);
      if (ChimericMsOption.SKIP.equals(handleChimericsOption)) {
        scans.removeIf(scan -> ChimericPrecursorFlag.CHIMERIC == chimericMap.get(scan).flag());
      }
      return chimericMap;
    }
    return Map.of();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureLists);
  }
}
