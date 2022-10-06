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

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusExportTask extends AbstractTask {

  // based on netID
  public static final String COMPOUND_ID = "COMPOUND_ID=";
  // based on feature shape correlation (metaMSEcorr)
  public static final String CORR_GROUPID = "CORR_GROUPID=";
  // neutral mass
  public static final String COMPOUND_MASS = "COMPOUND_MASS=";
  // ION
  public static final String ION = "ION=";

  private static final Logger logger = Logger.getLogger(SiriusExportTask.class.getName());
  private static final String plNamePattern = "{}";
  private final FeatureList[] featureLists;
  private final File fileName;
  private final boolean mergeEnabled;
  private final MsMsSpectraMergeParameters mergeParameters;
  private final MZTolerance mzTol;
  private final boolean excludeEmptyMSMS;
  private final boolean excludeMultiCharge;
  private final boolean excludeMultimers;
  private final boolean needAnnotation;
  private final boolean renumberID;
  private final double minimumRelativeNumberOfScans;
  // by robin
  private final NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // rows
  protected long finishedRows, totalRows;
  // next id for renumbering
  private long nextID = 1;

  SiriusExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(SiriusExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists();
    this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();
    this.mergeEnabled = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getValue();
    this.mergeParameters = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER)
        .getEmbeddedParameters();

    minimumRelativeNumberOfScans = mergeEnabled ? mergeParameters.getParameter(
        MsMsSpectraMergeParameters.REL_SIGNAL_COUNT_PARAMETER).getValue() : 0d;

    // new parameters related to ion identity networking and feature grouping
    mzTol = parameters.getParameter(SiriusExportParameters.MZ_TOL).getValue();
    excludeEmptyMSMS = parameters.getParameter(SiriusExportParameters.EXCLUDE_EMPTY_MSMS)
        .getValue();
    excludeMultiCharge = parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE)
        .getValue();
    excludeMultimers = parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTIMERS).getValue();
    needAnnotation = parameters.getParameter(SiriusExportParameters.NEED_ANNOTATION).getValue();
    // experimental
    renumberID = parameters.getParameter(SiriusExportParameters.RENUMBER_ID).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    return (totalRows == 0 ? 0.0 : (double) finishedRows / (double) totalRows);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to SIRIUS MGF file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    for (FeatureList l : featureLists) {
      this.totalRows += l.getNumberOfRows();
    }

    int totalExported = 0;
    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      File tmpFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        tmpFile = new File(newFilename);
      }
      final File curFile = FileAndPathUtil.getRealFilePath(tmpFile, "mgf");

      if (!FileAndPathUtil.createDirectory(curFile.getParentFile())) {
        setErrorMessage("Could not create directories for file " + curFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {
        logger.fine(() -> String.format("Exporting SIRIUS mgf for feature list: %s to file %s",
            featureList.getName(), curFile.getAbsolutePath()));
        totalExported += exportFeatureList(featureList, writer);

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING, String.format(
            "Error writing SIRIUS mgf format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }
    logger.info(String.format("SIRIUS: Exported %d features of %d total features", totalExported,
        totalRows));

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  public void runSingleRow(FeatureListRow row) {
    setStatus(TaskStatus.PROCESSING);
    try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
      exportFeatureListRow(row, bw);
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
    }
    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  public void runSingleRows(FeatureListRow[] rows) {
    setStatus(TaskStatus.PROCESSING);
    try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
      for (FeatureListRow row : rows) {
        exportFeatureListRow(row, bw);
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
    }
    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private int exportFeatureList(FeatureList featureList, BufferedWriter writer) throws IOException {
    int exported = 0;
    final FeatureListRow[] rows = featureList.getRows().toArray(FeatureListRow[]::new);
    for (FeatureListRow row : rows) {
      if (isCanceled()) {
        return exported;
      }

      IonIdentity adduct = row.getBestIonIdentity();
      boolean fitCharge = !excludeMultiCharge || row.getRowCharge() <= 1;
      boolean fitAnnotation = !needAnnotation || adduct != null;
      boolean fitMol =
          !excludeMultimers || adduct == null || adduct.getIonType().getMolecules() <= 1;
      if (fitAnnotation && fitCharge && fitMol && row.hasMs2Fragmentation()) {
        if (exportFeatureListRow(row, writer)) {
          exported++;
        }
      }
      finishedRows++;
    }
    return exported;
  }

  private boolean exportFeatureListRow(FeatureListRow row, BufferedWriter writer)
      throws IOException {
    // get row charge and polarity
    char polarity = 0;
    for (Feature f : row.getFeatures()) {
      if (f.getRepresentativeScan() == null) {
        continue;
      }
      char pol = f.getRepresentativeScan().getPolarity().asSingleChar().charAt(0);
      if (pol != polarity && polarity != 0) {
        setErrorMessage(
            "Joined features have different polarity. This is most likely a bug. If not, please separate them as individual features and/or write a feature request on github.");
        setStatus(TaskStatus.ERROR);
        return false;
      } else {
        polarity = pol;
      }
    }

    // MS annotation and feature correlation group
    // can be null (both)
    // run MS annotations module or better metaMSEcorrelate
    String msAnnotationsFlags = createMSAnnotationFlags(row, mzForm);

    // export MS1 of best feature
    if (!exportMS1Scan(row, writer, polarity, msAnnotationsFlags)) {
      return false;
    }

    if (mergeEnabled) {
      MergeMode mergeMode = mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE)
          .getValue();
      MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(MsMsSpectraMergeModule.class);

      switch (mergeMode) {
        case SAME_SAMPLE, CONSECUTIVE_SCANS:
          for (Feature f : row.getFeatures()) {
            if (f.getFeatureStatus() == FeatureStatus.DETECTED) {
              final Scan bestMS2 = f.getMostIntenseFragmentScan();
              if (bestMS2 == null) {
                continue;
              }
              if (missingMassListError(bestMS2, bestMS2.getMassList())) {
                return false;
              }
              if (excludeEmptyMSMS && bestMS2.getMassList().getNumberOfDataPoints() <= 0) {
                continue;
              }

              // write correlation spectrum
              exportCorrelationSpectrum(row, writer, polarity, msAnnotationsFlags, f);

              if (mergeMode == MergeMode.CONSECUTIVE_SCANS) {
                // merge MS/MS
                List<MergedSpectrum> spectra = merger.mergeConsecutiveScans(mergeParameters, f);
                for (MergedSpectrum spectrum : spectra) {
                  exportSpectrumIfNotEmpty(row, writer, polarity, msAnnotationsFlags, f,
                      spectrum.filterByRelativeNumberOfScans(minimumRelativeNumberOfScans));
                }
              } else {
                MergedSpectrum spectrum = merger.mergeFromSameSample(mergeParameters, f)
                    .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
                exportSpectrumIfNotEmpty(row, writer, polarity, msAnnotationsFlags, f, spectrum);
              }
            }
          }

        case ACROSS_SAMPLES:
          // write correlation spectrum
          exportCorrelationSpectrum(row, writer, polarity, msAnnotationsFlags,
              row.getBestFeature());
          // merge everything into one
          MergedSpectrum spectrum = merger.mergeAcrossSamples(mergeParameters, row)
              .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
          exportSpectrumIfNotEmpty(row, writer, polarity, msAnnotationsFlags, row.getBestFeature(),
              spectrum);
      }
    } else {
      // No merging

      // correlated ms1 features
      exportCorrelationSpectrum(row, writer, polarity, msAnnotationsFlags, row.getBestFeature());

      // export all MS2 scans
      for (Feature f : row.getFeatures()) {
        for (Scan ms2scan : f.getAllMS2FragmentScans()) {
          writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, ms2scan,
              msAnnotationsFlags);
          MassList ms2MassList = ms2scan.getMassList();
          if (missingMassListError(ms2scan, ms2MassList)) {
            return false;
          }
          if (excludeEmptyMSMS && ms2MassList.getNumberOfDataPoints() <= 0) {
            continue;
          }
          writeSpectrum(writer, ms2MassList.getDataPoints());
        }
      }
    }
    // for renumbering
    nextID++;
    return true;
  }

  /**
   * @return true if export successful false if mass list was missing
   */
  private boolean exportMS1Scan(FeatureListRow row, BufferedWriter writer, char polarity,
      String msAnnotationsFlags) throws IOException {
    Feature bestFeature = row.getBestFeature();
    final Scan representativeScan = bestFeature.getRepresentativeScan();
    MassList ms1MassList = representativeScan.getMassList();
    if (missingMassListError(representativeScan, ms1MassList)) {
      return false;
    }

    // ms1 scan
    writeHeader(writer, row, bestFeature.getRawDataFile(), polarity, MsType.MS, representativeScan,
        msAnnotationsFlags);
    writeSpectrum(writer, ms1MassList.getDataPoints());
    return true;
  }

  private boolean missingMassListError(Scan scan, MassList ms1MassList) {
    if (ms1MassList == null) {
      setErrorMessage("A mass list was missing for scan " + ScanUtils.scanToString(scan, true)
          + ". Maybe rerun mass detection on MS2 and MS1 without scan filtering (e.g., by retention time range).");
      setStatus(TaskStatus.ERROR);
      return true;
    }
    return false;
  }


  private void exportSpectrumIfNotEmpty(FeatureListRow row, BufferedWriter writer, char polarity,
      String msAnnotationsFlags, Feature f, MergedSpectrum spectrum) throws IOException {
    if (spectrum.data.length > 0) {
      writeHeaderForMerged(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, spectrum,
          msAnnotationsFlags);
      writeSpectrum(writer, spectrum.data);
    }
  }

  /**
   * Export the MS1 correlation spectrum of grouped features
   *
   * @throws IOException
   */
  private void exportCorrelationSpectrum(FeatureListRow row, BufferedWriter writer, char polarity,
      String msAnnotationsFlags, Feature f) throws IOException {
    if (row.getGroup() != null || f.getIsotopePattern() != null) {
      writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.CORRELATED, -1, null,
          msAnnotationsFlags);
      writeCorrelationSpectrum(writer, row, f.getRawDataFile());
    }
  }

  /**
   * Creates header for groupID, compoundGroupID compoundMass and ion annotation
   *
   * @param row
   * @return
   */
  public String createMSAnnotationFlags(FeatureListRow row, NumberFormat mzForm) {
    // MS annotation and feature correlation group
    // can be null (both)
    // run MS annotations module or better metaCorrelate
    RowGroup group = row.getGroup();
    IonIdentity adduct = row.getBestIonIdentity();
    IonNetwork net = adduct != null ? adduct.getNetwork() : null;

    StringBuilder b = new StringBuilder();
    if (group != null) {
      b.append(CORR_GROUPID).append(group.getGroupID()).append("\n");
    }
    if (net != null) {
      b.append(COMPOUND_ID).append(net.getID()).append("\n");
      b.append(COMPOUND_MASS).append(mzForm.format(net.calcNeutralMass())).append("\n");
    }
    if (adduct != null) {
      b.append(ION).append(adduct.getAdduct()).append("\n");
    }
    return b.toString();
  }


  private void writeHeaderForMerged(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, MergedSpectrum mergedSpectrum, String msAnnotationsFlags)
      throws IOException {
    writeHeader(writer, row, raw, polarity, msType, row.getID(),
        Arrays.stream(mergedSpectrum.origins).map(RawDataFile::getName)
            .collect(Collectors.toList()), msAnnotationsFlags);
    // add additional fields
    writer.write("MERGED_SCANS=");
    writer.write(String.valueOf(mergedSpectrum.scanIds[0]));
    for (int k = 1; k < mergedSpectrum.scanIds.length; ++k) {
      writer.write(',');
      writer.write(String.valueOf(mergedSpectrum.scanIds[k]));
    }
    writer.newLine();
    writer.write("MERGED_STATS=");
    writer.write(mergedSpectrum.getMergeStatsDescription());
    writer.newLine();
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, Scan scanNumber, String msAnnotationsFlags) throws IOException {
    writeHeader(writer, row, raw, polarity, msType, scanNumber.getScanNumber(), null,
        msAnnotationsFlags);
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, Integer scanNumber, List<String> sources,
      String msAnnotationsFlags) throws IOException {
    final Feature feature = row.getFeature(raw);
    writer.write("BEGIN IONS");
    writer.newLine();
    writer.write("FEATURE_ID=" + (renumberID ? nextID : row.getID()));
    writer.newLine();
    writer.write("PEPMASS=");
    writer.write(String.valueOf(row.getBestFeature().getMZ()));
    writer.newLine();
    // ion identity etc
    if (msAnnotationsFlags != null && !msAnnotationsFlags.isEmpty()) {
      writer.write(msAnnotationsFlags);
    }

    writer.write("CHARGE=");
    if (polarity == '-') {
      writer.write("-");
    }
    writer.write(String.valueOf(Math.abs(row.getRowCharge())));
    writer.newLine();
    writer.write("RTINSECONDS=");
    writer.write(String.valueOf(feature.getRT() * 60d));
    writer.newLine();
    switch (msType) {
      case CORRELATED:
        writer.write("SPECTYPE=CORRELATED MS");
        writer.newLine();
      case MS:
        writer.write("MSLEVEL=1");
        writer.newLine();
        break;
      case MSMS:
        writer.write("MSLEVEL=2");
        writer.newLine();
    }
    writer.write("FILENAME=");
    if (sources != null && !sources.isEmpty()) {
      final String[] uniqSources = new HashSet<>(sources).toArray(new String[0]);
      writer.write(escape(uniqSources[0], ";"));
      for (int i = 1; i < uniqSources.length; ++i) {
        writer.write(";");
        writer.write(escape(uniqSources[i], ";"));
      }
      writer.newLine();
    } else if (msType == MsType.CORRELATED) {
      RawDataFile[] raws = row.getRawDataFiles().toArray(new RawDataFile[0]);
      final Set<String> set = new HashSet<>();
      for (RawDataFile f : raws) {
        set.add(f.getName());
      }
      final String[] uniqSources = set.toArray(new String[0]);
      writer.write(escape(uniqSources[0], ";"));
      for (int i = 1; i < uniqSources.length; ++i) {
        writer.write(";");
        writer.write(escape(uniqSources[i], ";"));
      }
      writer.newLine();
    } else {
      writer.write(feature.getRawDataFile().getName());
      writer.newLine();
    }
    if (scanNumber != -1) {
      writer.write("SCANS=");
      writer.write(String.valueOf(scanNumber));
      writer.newLine();
    }
  }

  private void writeCorrelationSpectrum(BufferedWriter writer, FeatureListRow row, RawDataFile file)
      throws IOException {
    List<DataPoint> dps = generateCorrelationSpectrum(row, file);
    if (dps != null) {
      writeSpectrum(writer, dps.toArray(DataPoint[]::new));
    } else {
      // write nothing
      writer.write(String.valueOf(row.getFeature(file).getMZ()));
      writer.write(' ');
      writer.write("100.0");
      writer.newLine();
      writer.write("END IONS");
      writer.newLine();
      writer.newLine();
    }
  }

  /**
   * Generates a spectrum of all correlated features, such as isotope patterns and adducts assigned
   * via IIN (+ their isotopes).
   */
  @Nullable
  private List<DataPoint> generateCorrelationSpectrum(@NotNull FeatureListRow row,
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
      addIsotopePattern(feature, dps, true, ip);
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
          addIsotopePattern(sameFileFeature, dps, false, sameFileFeature.getIsotopePattern());
        }
      }
    }

    dps.sort(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));
    return dps.isEmpty() ? null : dps;
  }

  /**
   * Adds the isotopic peaks of this row to the list of data points.
   */
  private void addIsotopePattern(@NotNull Feature feature, @NotNull List<DataPoint> dps,
      boolean exportMolecularIon, @Nullable IsotopePattern ip) {
    if (ip != null) {
      for (int i = 0; i < ip.getNumberOfDataPoints(); i++) {
        // make sure to not export the molecular ion twice. Mass might change a bit due to smoothing
        if (mzTol.checkWithinTolerance(feature.getMZ(), ip.getMzValue(i)) && exportMolecularIon) {
          dps.add(new SimpleDataPoint(ip.getMzValue(i), ip.getIntensityValue(i)));
        }
      }
    }
  }

  private void writeSpectrum(BufferedWriter writer, DataPoint[] spectrum) throws IOException {
    for (DataPoint dataPoint : spectrum) {
      writer.write(mzForm.format(dataPoint.getMZ()));
      writer.write(' ');
      writer.write(intensityForm.format(dataPoint.getIntensity()));
      if (dataPoint instanceof AnnotatedDataPoint adp && adp.getAnnotation() != null
          && !adp.getAnnotation().isEmpty()) {
        // write the correlation between rows. for now 1 as it is not really used
        writer.write(" 1 ");
        writer.write(adp.getAnnotation());
      }
      writer.newLine();
    }
    writer.write("END IONS");
    writer.newLine();
    writer.newLine();
  }

  private String escape(String name, String s) {
    return name.replaceAll(s, "\\" + s);
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
