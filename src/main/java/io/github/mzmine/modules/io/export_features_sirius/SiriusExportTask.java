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

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.modules.tools.msmsspectramerge.MergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
  private final boolean mergeEnabled;
  private final MsMsSpectraMergeParameters mergeParameters;
  private final double minimumRelativeNumberOfScans;
  private final MZTolerance mzTol;
  private final Boolean excludeMultiCharge;
  private final Boolean excludeMultimers;
  private final Boolean needAnnotation;
  private final int totalRows;
  private final NumberFormats format = MZmineCore.getConfiguration().getExportFormats();
  private final MergeMode mergeMode;
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private final AtomicInteger processedRows = new AtomicInteger(0);


  protected SiriusExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.parameters = parameters;
    this.featureLists = parameters.getValue(SiriusExportParameters.FEATURE_LISTS)
        .getMatchingFeatureLists();
    this.fileName = parameters.getValue(SiriusExportParameters.FILENAME);
    this.mergeEnabled = parameters.getValue(SiriusExportParameters.MERGE_PARAMETER);
    this.mergeParameters = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER)
        .getEmbeddedParameters();

    minimumRelativeNumberOfScans = mergeEnabled ? mergeParameters.getParameter(
        MsMsSpectraMergeParameters.REL_SIGNAL_COUNT_PARAMETER).getValue() : 0d;

    // new parameters related to ion identity networking and feature grouping
    mzTol = parameters.getValue(SiriusExportParameters.MZ_TOL);
    excludeMultiCharge = parameters.getValue(SiriusExportParameters.EXCLUDE_MULTICHARGE);
    excludeMultimers = parameters.getValue(SiriusExportParameters.EXCLUDE_MULTIMERS);
    needAnnotation = parameters.getValue(SiriusExportParameters.NEED_ANNOTATION);
    mergeMode = mergeParameters.getValue(MsMsSpectraMergeParameters.MERGE_MODE);
    // experimental

    totalRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  private static void putMergedSpectrumFieldsIntoEntry(MergedSpectrum spectrum,
      SpectralLibraryEntry entry) {
    entry.putIfNotNull(DBEntryField.FILENAME,
        Arrays.stream(spectrum.origins).map(RawDataFile::getName).collect(Collectors.joining(";")));
    entry.putIfNotNull(DBEntryField.SIRIUS_MERGED_SCANS,
        Arrays.stream(spectrum.scanIds).mapToObj(Integer::toString)
            .collect(Collectors.joining(",")));
    entry.putIfNotNull(DBEntryField.SIRIUS_MERGED_STATS, spectrum.getMergeStatsDescription());
  }

  private static void putFeatureFieldsIntoEntry(Feature f, SpectralLibraryEntry entry) {
    int charge = 1;
    PolarityType polarity = f.getRepresentativeScan().getPolarity();
    if (f.getRow().getRowCharge() != null) {
      charge = f.getRow().getRowCharge();
    } else if (f.getMostIntenseFragmentScan().getMsMsInfo() instanceof DDAMsMsInfo dda) {
      charge = dda.getPrecursorCharge() != null ? dda.getPrecursorCharge() : charge;
    }
    charge = Math.max(charge, 1); // no zero charge

    entry.putIfNotNull(DBEntryField.FEATURE_ID, f.getRow().getID());
    // replicate what GNPS does - some tools rely on the scan number to be there
    // GNPS just uses the FEATURE_ID for this
    entry.putIfNotNull(DBEntryField.SCAN_NUMBER, f.getRow().getID());
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, f.getMZ());
    entry.putIfNotNull(DBEntryField.RT, f.getRT());
    entry.setCharge(charge, polarity);
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

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      if (isCanceled()) {
        return;
      }

      // Filename
      final File curFile = getFileForFeatureList(isSubstitute(), featureList);
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

  protected boolean isSubstitute() {
    boolean substitute = fileName.getPath().contains(MULTI_NAME_PATTERN);
    return substitute;
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
    final MassSpectrum correlated = generateCorrelationSpectrum(row, null);
    if (correlated != null && correlated.getNumberOfDataPoints() > 1) {
      entries.add(spectrumToEntry(MsType.CORRELATED, correlated, row.getBestFeature()));
    } else {
      // export best MS1
      entries.add(spectrumToEntry(MsType.MS, row.getBestFeature().getRepresentativeScan(),
          row.getBestFeature()));
    }

    if (mergeEnabled) {
      final List<SpectralLibraryEntry> ms2Entries = getMergedMs2SpectraEntries(mergeMode, row);
      if (ms2Entries != null) {
        entries.addAll(ms2Entries);
      }
    } else {
      final List<SpectralLibraryEntry> ms2Entries = row.streamFeatures().flatMap(
              f -> f.getAllMS2FragmentScans().stream().map(s -> spectrumToEntry(MsType.MSMS, s, f)))
          .toList();
      entries.addAll(ms2Entries);
    }

    if (entries.size() < 2) {
      // only MS1
      return false;
    }

    for (SpectralLibraryEntry entry : entries) {
      final String mgfEntry = MGFEntryGenerator.createMGFEntry(entry);
      writer.write(mgfEntry);
      writer.newLine();
    }
    return true;
  }

  public SpectralLibraryEntry spectrumToEntry(MsType spectrumType, MassSpectrum spectrum,
      Feature f) {

    // TODO MSAnnotationFlags from old sirius import
    final SpectralLibraryEntry entry = switch (spectrum) {
      case MergedSpectrum spec -> SpectralLibraryEntry.create(null, f.getMZ(), spec.data);
      case Scan scan ->
          SpectralLibraryEntry.create(null, f.getMZ(), ScanUtils.extractDataPoints(scan, true));
      case SimpleMassSpectrum spec ->
          SpectralLibraryEntry.create(null, f.getMZ(), ScanUtils.extractDataPoints(spec));
      default -> throw new IllegalStateException(
          "Cannot extract data points from spectrum class " + spectrum.getClass().getName());
    };

    putFeatureFieldsIntoEntry(f, entry);

    switch (spectrumType) {
      case CORRELATED -> {
        entry.putIfNotNull(DBEntryField.MS_LEVEL, 1);
        entry.putIfNotNull(DBEntryField.MERGED_SPEC_TYPE, "CORRELATED MS");
        entry.putIfNotNull(DBEntryField.FILENAME,
            f.getRow().getFeatures().stream().map(Feature::getRawDataFile).filter(Objects::nonNull)
                .map(RawDataFile::getName).collect(Collectors.joining(";")));
      }
      case MS -> entry.putIfNotNull(DBEntryField.MS_LEVEL, 1);
      case MSMS -> entry.putIfNotNull(DBEntryField.MS_LEVEL, 2);
    }

    final IonIdentity ionType = f.getRow().getBestIonIdentity();
    if (ionType != null) {
      entry.putIfNotNull(DBEntryField.ION_TYPE, ionType.getAdduct());
    }

    if (spectrum instanceof MergedSpectrum spec) {
      putMergedSpectrumFieldsIntoEntry(spec, entry);
    }

    return entry;
  }

  private List<SpectralLibraryEntry> getMergedMs2SpectraEntries(MergeMode mergeMode,
      FeatureListRow row) {

    List<SpectralLibraryEntry> entries = new ArrayList<>();
    final MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(
        MsMsSpectraMergeModule.class);

    switch (mergeMode) {
      case SAME_SAMPLE -> {
        for (Feature f : row.getFeatures()) {
          final Scan bestMS2 = f.getMostIntenseFragmentScan();
          if (bestMS2 == null) {
            continue;
          }
          if (bestMS2.getMassList() == null) {
            throw new MissingMassListException(bestMS2);
          }
          if (bestMS2.getMassList().getNumberOfDataPoints() <= 0) {
            continue;
          }

          MergedSpectrum spectrum = merger.mergeFromSameSample(mergeParameters, f)
              .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
          entries.add(spectrumToEntry(MsType.MSMS, spectrum, f));
        }
      }

      case CONSECUTIVE_SCANS -> {
        for (Feature f : row.getFeatures()) {
          final Scan bestMS2 = f.getMostIntenseFragmentScan();
          if (bestMS2 == null) {
            continue;
          }
          if (bestMS2.getMassList() == null) {
            throw new MissingMassListException(bestMS2);
          }
          if (bestMS2.getMassList().getNumberOfDataPoints() <= 0) {
            continue;
          }

          final List<MergedSpectrum> mergedSpectra = merger.mergeConsecutiveScans(mergeParameters,
              f);
          for (MergedSpectrum spectrum : mergedSpectra) {
            entries.add(spectrumToEntry(MsType.MSMS,
                spectrum.filterByRelativeNumberOfScans(minimumRelativeNumberOfScans), f));
          }
        }
      }

      case ACROSS_SAMPLES -> {
        // merge everything into one
        MergedSpectrum spectrum = merger.mergeAcrossSamples(mergeParameters, row)
            .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
        entries.add(spectrumToEntry(MsType.MSMS, spectrum, row.getBestFeature()));
      }
    }

    entries.removeIf(e -> e.getNumberOfDataPoints() == 0);
    return entries;
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
  protected File getFileForFeatureList(boolean substitute, FeatureList featureList) {
    File tmpFile = fileName;
    if (substitute) {
      // Cleanup from illegal filename characters
      String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
      // Substitute
      String newFilename = fileName.getPath()
          .replaceAll(Pattern.quote(MULTI_NAME_PATTERN), cleanPlName);
      tmpFile = new File(newFilename);
    }
    final File curFile = FileAndPathUtil.getRealFilePath(tmpFile, "mgf");

    if (!FileAndPathUtil.createDirectory(curFile.getParentFile())) {
      return null;
    }
    return curFile;
  }

  /**
   * Generates a spectrum of all correlated features, such as isotope patterns and adducts assigned
   * via IIN (+ their isotopes).
   */
  @Nullable
  private MassSpectrum generateCorrelationSpectrum(@NotNull FeatureListRow row,
      @Nullable RawDataFile file) {
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

    dps.sort(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));
    removeDuplicateDataPoints(dps,
        mzTol); // remove duplicate isotope peaks (might be correlated features too)
    final double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return dps.isEmpty() ? null : new SimpleMassSpectrum(dp[0], dp[1]);
  }

  /**
   * Adds the isotopic peaks of this row to the list of data points.
   */
  private void addIsotopePattern(@NotNull Feature feature, @NotNull List<DataPoint> dps,
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
   * @param sortedDp  data points sorted by mz.
   * @param mzTol MZ tolerance to filter equal data points.
   */
  private void removeDuplicateDataPoints(List<DataPoint> sortedDp, MZTolerance mzTol) {
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

  private enum MsType {
    /**
     * Describes the original MS1 spectrum
     */
    MS,
    /**
     * The MS2 spectrum, either merged raw spectra, or the best raw spectrum.
     */
    MSMS,
    /**
     * Only contains m/zs of features that correlate with this feature. (e.g. isotopic signals or
     * different adducts).
     */
    CORRELATED
  }
}
