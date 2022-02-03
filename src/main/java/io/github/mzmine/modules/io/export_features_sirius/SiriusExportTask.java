/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

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
  private final Boolean excludeMultimers;
  private final Boolean needAnnotation;
  // rows
  protected long finishedRows, totalRows;
  // by robin
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // seconds
  private NumberFormat rtsForm = new DecimalFormat("0.###");
  // correlation
  private NumberFormat corrForm = new DecimalFormat("0.0000");
  // next id for renumbering
  private int nextID = 1;
  private boolean renumberID;

  SiriusExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(SiriusExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists();
    this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();
    this.mergeEnabled = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getValue();
    this.mergeParameters =
        parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getEmbeddedParameters();

    // new parameters related to ion identity networking and feature grouping
    mzTol = parameters.getParameter(SiriusExportParameters.MZ_TOL).getValue();
    excludeEmptyMSMS =
        parameters.getParameter(SiriusExportParameters.EXCLUDE_EMPTY_MSMS).getValue();
    excludeMultiCharge =
        parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE).getValue();
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
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MGF file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    for (FeatureList l : featureLists) {
      this.totalRows += l.getNumberOfRows();
      prefillStatistics(l.getRows().toArray(FeatureListRow[]::new));
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);

      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, ".mgf");

      // Open file
      try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile))) {
        exportFeatureList(featureList, bw);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

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
    // prefill statistics
    prefillStatistics(rows);
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

  private void prefillStatistics(FeatureListRow[] rows) {
    ArrayList<FeatureListRow> copy = new ArrayList<>(Arrays.asList(rows));
    Collections.shuffle(copy);
  }

  private int exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {
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
      if (fitAnnotation && fitCharge && fitMol && hasMsMsOrIsotopes(row)) {
        if (exportFeatureListRow(row, writer)) {
          exported++;
        }
      }
      finishedRows++;
    }
    return exported;
  }

  private boolean exportFeatureListRow(FeatureListRow row, BufferedWriter writer) throws IOException {
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

    if (mergeEnabled) {
      MergeMode mergeMode =
          mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
      MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(MsMsSpectraMergeModule.class);
      if (mergeMode != MergeMode.ACROSS_SAMPLES) {
        for (Feature f : row.getFeatures()) {
          if (f.getFeatureStatus() == FeatureStatus.DETECTED
              && f.getMostIntenseFragmentScan() != null) {
            // write correlation spectrum
            writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.CORRELATED, -1, null, msAnnotationsFlags);
            writeCorrelationSpectrum(writer, f);
            if (mergeMode == MergeMode.CONSECUTIVE_SCANS) {
              // merge MS/MS
              List<MergedSpectrum> spectra =
                  merger.mergeConsecutiveScans(mergeParameters, f);
              for (MergedSpectrum spectrum : spectra) {
                writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS,
                    spectrum.filterByRelativeNumberOfScans(mergeParameters
                        .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER)
                        .getValue()), msAnnotationsFlags);
                writeSpectrum(writer, spectrum.data);
              }
            } else {
              MergedSpectrum spectrum = merger.mergeFromSameSample(mergeParameters, f)
                  .filterByRelativeNumberOfScans(mergeParameters
                      .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER).getValue());
              if (spectrum.data.length > 0) {
                writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, spectrum, msAnnotationsFlags);
                writeSpectrum(writer, spectrum.data);
              }
            }
          }
        }
      } else {
        // write correlation spectrum
        writeHeader(writer, row, row.getBestFeature().getRawDataFile(), polarity, MsType.CORRELATED,
            -1, null, msAnnotationsFlags);
        writeCorrelationSpectrum(writer, row.getBestFeature());
        // merge everything into one
        MergedSpectrum spectrum = merger.mergeAcrossSamples(mergeParameters, row)
            .filterByRelativeNumberOfScans(mergeParameters
                .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER).getValue());
        if (spectrum.data.length > 0) {
          writeHeader(writer, row, row.getBestFeature().getRawDataFile(), polarity, MsType.MSMS,
              spectrum, msAnnotationsFlags);
          writeSpectrum(writer, spectrum.data);
        }
      }
    } else {
      // No merging
      Feature bestFeature = row.getBestFeature();
      MassList ms1MassList = bestFeature.getRepresentativeScan().getMassList();
      if(ms1MassList==null) {
        setErrorMessage("A mass list was missing for scan "
                        + ScanUtils.scanToString(bestFeature.getRepresentativeScan(), true)
                        + ". Maybe rerun mass detection on MS2 and MS1 without scan filtering (e.g., by retention time range).");
        setStatus(TaskStatus.ERROR);
        return false;
      }
        writeHeader(writer, row, bestFeature.getRawDataFile(), polarity, MsType.MS,
            bestFeature.getRepresentativeScan(), msAnnotationsFlags);
        writeSpectrum(writer, ms1MassList.getDataPoints());

      for (Feature f : row.getFeatures()) {
        for (Scan ms2scan : f.getAllMS2FragmentScans()) {
          writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, ms2scan, msAnnotationsFlags);
          MassList ms2MassList = ms2scan.getMassList();
          if (ms2MassList == null || (excludeEmptyMSMS && ms2MassList.getNumberOfDataPoints()<=0)) {
            continue;
          }
          writeSpectrum(writer, ms2MassList.getDataPoints());
        }
      }

    }
    nextID++;
    return true;
  }


  /**
   * Creates header for groupID, compoundGroupID compoundMass and ion annotation
   *
   * @param row
   * @return
   */
  public static String createMSAnnotationFlags(FeatureListRow row, NumberFormat mzForm) {
    // MS annotation and feature correlation group
    // can be null (both)
    // run MS annotations module or better metaMSEcorrelate
    RowGroup group = row.getGroup();
    IonIdentity adduct = row.getBestIonIdentity();
    IonNetwork net = adduct != null ? adduct.getNetwork() : null;

    // find ion species by annotation (can be null)
    String corrGroupID = group != null ? "" + group.getGroupID() : "";

    String ion = "";
    String compoundGroupID = "";
    String compoundMass = "";
    if (adduct != null) {
      ion = adduct.getAdduct();
    }
    if (net != null) {
      compoundGroupID = net.getID() + "";
      compoundMass = mzForm.format(net.calcNeutralMass());
    }

    StringBuilder b = new StringBuilder();
    if (!corrGroupID.isEmpty())
      b.append(CORR_GROUPID + corrGroupID + "\n");
    if (!compoundGroupID.isEmpty())
      b.append(COMPOUND_ID + compoundGroupID + "\n");
    if (!compoundMass.isEmpty())
      b.append(COMPOUND_MASS + compoundMass + "\n");
    if (!ion.isEmpty())
      b.append(ION + ion + "\n");
    return b.toString();
  }

  private boolean hasMsMsOrIsotopes(FeatureListRow row) {
    // skip rows which have no isotope pattern and no MS/MS spectrum
    for (Feature f : row.getFeatures()) {
      if (f.getFeatureStatus() == FeatureStatus.DETECTED) {
        // has isotope pattern or MS2
        if ((f.getIsotopePattern() != null && f.getIsotopePattern().getNumberOfDataPoints() > 1)
            || f.getMostIntenseFragmentScan() != null) {
          return true;
        }
      }
    }
    return false;
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, MergedSpectrum mergedSpectrum, String msAnnotationsFlags) throws IOException {
    writeHeader(writer, row, raw, polarity, msType, row.getID(), Arrays
        .stream(mergedSpectrum.origins).map(RawDataFile::getName).collect(Collectors.toList()), msAnnotationsFlags);
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
    writeHeader(writer, row, raw, polarity, msType, scanNumber.getScanNumber(), null, msAnnotationsFlags);
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, Integer scanNumber, List<String> sources, String msAnnotationsFlags) throws IOException {
    final Feature feature = row.getFeature(raw);
    writer.write("BEGIN IONS");
    writer.newLine();
    writer.write("FEATURE_ID=");
    writer.write(String.valueOf(row.getID()));
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
    if (sources != null) {
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

  private void writeCorrelationSpectrum(BufferedWriter writer, Feature feature) throws IOException {
    if (feature.getIsotopePattern() != null) {
      writeSpectrum(writer, ScanUtils.extractDataPoints(feature.getIsotopePattern()));
    } else {
      // write nothing
      writer.write(String.valueOf(feature.getMZ()));
      writer.write(' ');
      writer.write("100.0");
      writer.newLine();
      writer.write("END IONS");
      writer.newLine();
      writer.newLine();
    }
  }

  private void writeSpectrum(BufferedWriter writer, DataPoint[] spectrum) throws IOException {
    for (int i = 0; i < spectrum.length; i++) {
      writer.write(String.valueOf(spectrum[i].getMZ()));
      writer.write(' ');
      writer.write(intensityForm.format(spectrum[i].getIntensity()));
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
    MS, MSMS, CORRELATED
  }

}
