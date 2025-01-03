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

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusExportTask extends AbstractTask {

  public static final String MULTI_NAME_PATTERN = "{}";
  private static final Logger logger = Logger.getLogger(SiriusExportTask.class.getName());
  private final ParameterSet parameters;
  private final ModularFeatureList[] featureLists;
  private final File fileName;
  private final MZTolerance mzTol;
  private final Boolean excludeMultiCharge;
  private final Boolean excludeMultimers;
  private final Boolean needAnnotation;
  private final int totalRows;
  private final NumberFormats format = MZmineCore.getConfiguration().getExportFormats();
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private final AtomicInteger processedRows = new AtomicInteger(0);
  private final IntensityNormalizer normalizer;
  private final SpectralLibraryEntryFactory entryFactory;
  private final FragmentScanSelection scanMergeSelect;


  protected SiriusExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.parameters = parameters;
    this.featureLists = parameters.getValue(SiriusExportParameters.FEATURE_LISTS)
        .getMatchingFeatureLists();
    this.fileName = parameters.getValue(SiriusExportParameters.FILENAME);
    var mergeSelect = parameters.getParameter(SiriusExportParameters.spectraMergeSelect)
        .getValueWithParameters();

    this.scanMergeSelect = mergeSelect.value()
        .createFragmentScanSelection(getMemoryMapStorage(), mergeSelect.parameters());

    // new parameters related to ion identity networking and feature grouping
    mzTol = parameters.getValue(SiriusExportParameters.MZ_TOL);
    normalizer = parameters.getValue(SiriusExportParameters.NORMALIZE);
    excludeMultiCharge = parameters.getValue(SiriusExportParameters.EXCLUDE_MULTICHARGE);
    excludeMultimers = parameters.getValue(SiriusExportParameters.EXCLUDE_MULTIMERS);
    needAnnotation = parameters.getValue(SiriusExportParameters.NEED_ANNOTATION);

    totalRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();

    entryFactory = new SpectralLibraryEntryFactory(true, true, true, false);
    entryFactory.setAddOnlineReactivityFlags(true);
  }

  @Override
  public String getTaskDescription() {
    return "Running sirius export for feature list(s) " + Arrays.stream(featureLists)
        .map(FeatureList::getName).collect(Collectors.joining(", ")) + ".";
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows.get() / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (featureLists.length > 1 && !SiriusExportTask.hasDefaultSubstitutionPattern(fileName)) {
      // error that multiple feature lists are selected and no filename pattern defined
      error("""
          Multiple feature lists (%d) were selected for Sirius export, /
          but the filename misses the file name pattern "%s" to insert each feature list name.
          Either select a single feature list or use the name pattern.""".formatted(
          featureLists.length, SiriusExportTask.MULTI_NAME_PATTERN));
      return;
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      if (isCanceled()) {
        return;
      }

      // Filename
      final File curFile = getFileForFeatureList(featureList);
      if (curFile == null) {
        setErrorMessage("Could not create directories for file " + curFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {
        logger.fine(() -> String.format("Exporting SIRIUS mgf for feature list: %s to file %s",
            featureList.getName(), curFile.getAbsolutePath()));
        exportFeatureList(featureList, writer);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING, String.format(
            "Error writing SIRIUS mgf format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      }
    }

    for (ModularFeatureList featureList : featureLists) {
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(SiriusExportModule.class, parameters,
              getModuleCallDate()));
    }

    logger.info(
        "Processed " + processedRows.get() + " rows, exported " + exportedRows.get() + " rows.");

    setStatus(TaskStatus.FINISHED);
  }

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {

    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return;
      }

      if (exportRow(writer, row)) {
        exportedRows.getAndIncrement();
      }

      processedRows.getAndIncrement();
    }
  }

  /**
   * @return True if the row was exported.
   */
  public boolean exportRow(BufferedWriter writer, FeatureListRow row) throws IOException {

    if (!checkFeatureCriteria(row)) {
      return false;
    }

    // Use SpectralLibraryEntry to easily generate MGF files
    final List<SpectralLibraryEntry> entries = new ArrayList<>();

    // export either correlated OR MS1
    final SpectralLibraryEntry ms1 = getCorrelatedOrBestMS1Spectrum(row);
    if (ms1 != null) {
      entries.add(ms1);
    }

    // merge and select scans - transform into merged spectra
    for (final Scan scan : scanMergeSelect.getAllFragmentSpectra(row)) {
      entries.add(spectrumToEntry(scan, row, null));
    }

    if (entries.size() < 2) {
      // only one MS1 scan
      return false;
    }

    int actuallyExported = 0;
    for (SpectralLibraryEntry entry : entries) {
      final var mgfEntry = MGFEntryGenerator.createMGFEntry(entry, normalizer);
      if (mgfEntry.numSignals() > 0) {
        writer.write(mgfEntry.spectrum());
        writer.newLine();
        actuallyExported++;
      }
    }
    return actuallyExported > 0;
  }


  private @Nullable SpectralLibraryEntry getCorrelatedOrBestMS1Spectrum(final FeatureListRow row) {
    final Feature bestFeature = row.getBestFeature();
    if (bestFeature == null) {
      // maybe no MS1 data?
      logger.warning(
          "Cannot export MS1 data for this feature list. This maybe due to missing MS1 data or unsupported workflow. mzmine will skip MS1 scan of row "
          + FeatureUtils.rowToString(row));
      return null;
    }

    final SpectralLibraryEntry correlated = generateCorrelationSpectrum(entryFactory, mzTol, row,
        null, null);
    if (correlated != null && correlated.getNumberOfDataPoints() > 1) {
      return correlated;
    } else {
      // export best MS1
      var ms1Scan = bestFeature.getRepresentativeScan();
      if (ms1Scan == null) {
        logger.fine(
            "Best feature has no representative scan. This may be due to missing MS1 data or unsupported workflow. mzmine will skip MS1 scan of row "
            + FeatureUtils.rowToString(row));
        return null;
      }
      return spectrumToEntry(ms1Scan, row, bestFeature);
    }
  }

  public SpectralLibraryEntry spectrumToEntry(MassSpectrum spectrum,
      final @Nullable FeatureListRow row, final @Nullable Feature f) {
    final DataPoint[] data = ScanUtils.extractDataPoints(spectrum, true);

    // create unknown to not interfere with annotation by sirius by adding to much info
    final SpectralLibraryEntry entry = entryFactory.createUnknown(null, row, f, spectrum, data,
        null, null);
    // below here are only SIRIUS specific fields added or overwritten.
    // all default behavior should go into {@link SpectralLibraryEntryFactory}

    return entry;
  }

  private boolean checkFeatureCriteria(final FeatureListRow row) {

    if (!row.hasMs2Fragmentation()) {
      return false;
    }

    if (excludeMultiCharge && Math.abs(row.getRowCharge()) > 1) {
      return false;
    }

    IonIdentity adduct = row.getBestIonIdentity();
    if (needAnnotation && adduct == null) {
      return false;
    }

    return !excludeMultimers || adduct == null || adduct.getIonType().getMolecules() <= 1;
  }

  @Nullable
  public File getFileForFeatureList(FeatureList featureList) {
    return SiriusExportTask.getFileForFeatureList(featureList, fileName, MULTI_NAME_PATTERN, "mgf");
  }

  /**
   * Uses {} as a pattern to fill in feature list name. If file is prefix_{}_some_suffix the feature
   * list name will be inserted and file format appended, resulting in:
   * <p>
   * prefix_FEATURELISTNAME_some_suffix.mgf (if format is defined as mgf)
   *
   * @param featureList   the feature list name will be added into the substitution pattern
   * @param file          the initial file name to be modified
   * @param fileExtension file format to apply
   * @return replaced file
   */
  @Nullable
  public static File getFileForFeatureList(FeatureList featureList, File file,
      final @Nullable String fileExtension) {
    return getFileForFeatureList(featureList, file, MULTI_NAME_PATTERN, fileExtension);
  }

  /**
   * if file is prefix_{}_some_suffix and the namePattern is defined as {} the feature list name
   * will be inserted and file format appended, resulting in:
   * <p>
   * prefix_FEATURELISTNAME_some_suffix.mgf (if format is defined as mgf)
   *
   * @param featureList   the feature list name will be added into the substitution pattern
   * @param file          the initial file name to be modified
   * @param namePattern   the name pattern to be replaced with the feature list name. usually {}
   * @param fileExtension file format to apply
   * @return replaced file
   */
  @Nullable
  public static File getFileForFeatureList(FeatureList featureList, File file,
      final String namePattern, final @Nullable String fileExtension) {
    final boolean substitute = hasSubstitutionPattern(file, namePattern);
    if (substitute) {
      // Cleanup from illegal filename characters
      String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
      // Substitute
      String newFilename = file.getPath().replaceAll(Pattern.quote(namePattern), cleanPlName);
      file = new File(newFilename);
    }
    if (fileExtension != null) {
      file = FileAndPathUtil.getRealFilePath(file, fileExtension);
    }

    if (!FileAndPathUtil.createDirectory(file.getParentFile())) {
      return null;
    }
    return file;
  }

  public static boolean hasDefaultSubstitutionPattern(final File file) {
    return hasSubstitutionPattern(file, MULTI_NAME_PATTERN);
  }

  public static boolean hasSubstitutionPattern(final File file, final String namePattern) {
    return file.getPath().contains(namePattern);
  }

  /**
   * Generates a spectrum of all correlated features, such as isotope patterns and adducts assigned
   * via IIN (+ their isotopes).
   */
  @Nullable
  public static SpectralLibraryEntry generateCorrelationSpectrum(
      final SpectralLibraryEntryFactory entryFactory, final MZTolerance mzTol,
      @NotNull FeatureListRow row, @Nullable RawDataFile file,
      @Nullable final Map<DBEntryField, Object> metadataMap) {
    file = file != null ? file : row.getBestFeature().getRawDataFile();
    final List<DataPoint> dps = new ArrayList<>();

    final Feature feature = row.getFeature(file);
    if (feature == null) {
      return null;
    }

    final RowGroup group = row.getGroup();
    final IonIdentity identity = row.getBestIonIdentity();
    final IsotopePattern ip = feature.getIsotopePattern();

    if (group == null && identity != null) {
      throw new IllegalStateException("Cannot have an ion identity without a row group.");
    }

    if (group == null) {
      // add isotope pattern of this feature only if we don't have a group, otherwise the isotope
      // pattern is exported below.
      addIsotopePattern(feature, dps, ip);
    }

    if (group != null) {
      final IonNetwork network = identity != null ? identity.getNetwork() : null;
      for (final FeatureListRow groupedRow : group.getRows()) {
        // only write intensities of the same file, otherwise intensities will be distorted
        final Feature sameFileFeature = groupedRow.getFeature(file);
        if (sameFileFeature == null
            || sameFileFeature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          continue;
        }

        // this writes the data points of the row we want to export and all grouped rows + their isotope patterns.
        if (row.equals(groupedRow) || group.isCorrelated(row, groupedRow)) {
          // if we have an annotation, export the annotation
          if (network != null && network.get(groupedRow) != null) {
            dps.add(new AnnotatedDataPoint(sameFileFeature.getMZ(), sameFileFeature.getHeight(),
                network.get(groupedRow).getAdduct()));
          } else {
            dps.add(new SimpleDataPoint(sameFileFeature.getMZ(), sameFileFeature.getHeight()));
          }

          // add isotope pattern of correlated ions. The groupedRow ion has been added previously.
          addIsotopePattern(sameFileFeature, dps, sameFileFeature.getIsotopePattern());
        }
      }
    }

    dps.sort(DataPointSorter.DEFAULT_MZ_ASCENDING);
    // remove duplicate isotope peaks (might be correlated features too)
    removeDuplicateDataPoints(dps, mzTol);

    if (dps.size() <= 1) {
      return null; // empty or only self signal
    }

    final SpectralLibraryEntry entry = entryFactory.createUnknown(null, row, null, null,
        dps.toArray(DataPoint[]::new), null, metadataMap);

    entry.putIfNotNull(DBEntryField.MS_LEVEL, 1);
    entry.putIfNotNull(DBEntryField.MERGED_SPEC_TYPE, MergingType.CORRELATED_MS1);

    return entry;
  }

  /**
   * Adds the isotopic peaks of this row to the list of data points.
   */
  private static void addIsotopePattern(@NotNull Feature feature, @NotNull List<DataPoint> dps,
      @Nullable IsotopePattern ip) {
    if (ip != null) {
      for (int i = 0; i < ip.getNumberOfDataPoints(); i++) {
        dps.add(new SimpleDataPoint(ip.getMzValue(i), ip.getIntensityValue(i)));
      }
    }
  }

  /**
   * Removes duplicate data points from the given list. In this case, isotopic features may be among
   * the correlated ions and thus be added from the isotope pattern of the monoisotopic mass and as
   * a correlated ion.
   *
   * @param sortedDp data points sorted by mz.
   * @param mzTol    MZ tolerance to filter equal data points.
   */
  private static void removeDuplicateDataPoints(List<DataPoint> sortedDp, MZTolerance mzTol) {
    for (int i = sortedDp.size() - 2; i >= 0; i--) {
      if (mzTol.checkWithinTolerance(sortedDp.get(i).getMZ(), sortedDp.get(i + 1).getMZ())) {
        if (sortedDp.get(i) instanceof AnnotatedDataPoint) {
          sortedDp.remove(i + 1);
        } else {
          sortedDp.remove(i);
        }
      }
    }
  }


}
