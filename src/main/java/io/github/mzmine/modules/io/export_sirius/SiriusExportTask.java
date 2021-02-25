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

package io.github.mzmine.modules.io.export_sirius;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;

public class SiriusExportTask extends AbstractTask {

  private final static String plNamePattern = "{}";
  protected static final Comparator<DataPoint> CompareDataPointsByMz = new Comparator<DataPoint>() {
    @Override
    public int compare(DataPoint o1, DataPoint o2) {
      return Double.compare(o1.getMZ(), o2.getMZ());
    }
  };
  protected static final Comparator<DataPoint> CompareDataPointsByDecreasingInt =
      new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
          return Double.compare(o2.getIntensity(), o1.getIntensity());
        }
      };
  private final FeatureList[] featureLists;
  private final File fileName;
  protected long finishedRows, totalRows;

  private final boolean mergeEnabled;
  private final MsMsSpectraMergeParameters mergeParameters;

  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();

  @Override
  public double getFinishedPercentage() {
    return (totalRows == 0 ? 0.0 : (double) finishedRows / (double) totalRows);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MGF file(s)";
  }

  SiriusExportTask(ParameterSet parameters) {
    this.featureLists = parameters.getParameter(SiriusExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists();
    this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();
    this.mergeEnabled = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getValue();
    this.mergeParameters =
        parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getEmbeddedParameters();
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

      // Open file
      try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile))) {
        exportFeatureList(featureList, bw);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  public void runSingleRow(FeatureListRow row) {
    setStatus(TaskStatus.PROCESSING);
    try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
      exportFeatureListRow(row, bw);
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
    }
    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  public void runSingleRows(FeatureListRow[] rows) {
    setStatus(TaskStatus.PROCESSING);
    // prefill statistics
    prefillStatistics(rows);
    try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
      for (FeatureListRow row : rows)
        exportFeatureListRow(row, bw);
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
    }
    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void prefillStatistics(FeatureListRow[] rows) {
    ArrayList<FeatureListRow> copy = new ArrayList<>(Arrays.asList(rows));
    Collections.shuffle(copy);
  }

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {
    for (FeatureListRow row : featureList.getRows()) {
      if (!isSkipRow(row))
        exportFeatureListRow(row, writer);
      finishedRows++;
    }
  }

  private void exportFeatureListRow(FeatureListRow row, BufferedWriter writer) throws IOException {

    // get row charge and polarity
    char polarity = 0;
    for (Feature f : row.getFeatures()) {
      char pol = f.getRepresentativeScan().getPolarity().asSingleChar().charAt(0);
      if (pol != polarity && polarity != 0) {
        setErrorMessage(
            "Joined features have different polarity. This is most likely a bug. If not, please separate them as individual features and/or write a feature request on github.");
        setStatus(TaskStatus.ERROR);
        return;
      } else {
        polarity = pol;
      }
    }

    if (mergeEnabled) {
      MergeMode mergeMode =
          mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
      MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(MsMsSpectraMergeModule.class);
      if (mergeMode != MergeMode.ACROSS_SAMPLES) {
        for (Feature f : row.getFeatures()) {
          if (f.getFeatureStatus() == FeatureStatus.DETECTED
              && f.getMostIntenseFragmentScan() != null) {
            // write correlation spectrum
            writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.CORRELATED, -1, null);
            writeCorrelationSpectrum(writer, f);
            if (mergeMode == MergeMode.CONSECUTIVE_SCANS) {
              // merge MS/MS
              List<MergedSpectrum> spectra =
                  merger.mergeConsecutiveScans(mergeParameters, f);
              for (MergedSpectrum spectrum : spectra) {
                writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS,
                    spectrum.filterByRelativeNumberOfScans(mergeParameters
                        .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER)
                        .getValue()));
                writeSpectrum(writer, spectrum.data);
              }
            } else {
              MergedSpectrum spectrum = merger.mergeFromSameSample(mergeParameters, f)
                  .filterByRelativeNumberOfScans(mergeParameters
                      .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER).getValue());
              if (spectrum.data.length > 0) {
                writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, spectrum);
                writeSpectrum(writer, spectrum.data);
              }
            }
          }
        }
      } else {
        // write correlation spectrum
        writeHeader(writer, row, row.getBestFeature().getRawDataFile(), polarity, MsType.CORRELATED,
            -1, null);
        writeCorrelationSpectrum(writer, row.getBestFeature());
        // merge everything into one
        MergedSpectrum spectrum = merger.mergeAcrossSamples(mergeParameters, row)
            .filterByRelativeNumberOfScans(mergeParameters
                .getParameter(MsMsSpectraMergeParameters.FEATURE_COUNT_PARAMETER).getValue());
        if (spectrum.data.length > 0) {
          writeHeader(writer, row, row.getBestFeature().getRawDataFile(), polarity, MsType.MSMS,
              spectrum);
          writeSpectrum(writer, spectrum.data);
        }
      }
    } else {
      // No merging
      Feature bestFeature = row.getBestFeature();
      MassList ms1MassList = bestFeature.getRepresentativeScan().getMassList();
      if (ms1MassList != null) {
        writeHeader(writer, row, bestFeature.getRawDataFile(), polarity, MsType.MS,
            bestFeature.getRepresentativeScan());
        writeSpectrum(writer, ms1MassList.getDataPoints());
      }

      for (Feature f : row.getFeatures()) {
        for (Scan ms2scan : f.getAllMS2FragmentScans()) {
          writeHeader(writer, row, f.getRawDataFile(), polarity, MsType.MSMS, ms2scan);
          MassList ms2MassList = ms2scan.getMassList();
          if (ms2MassList == null)
            continue;
          writeSpectrum(writer, ms2MassList.getDataPoints());
        }
      }

    }
  }

  private boolean isSkipRow(FeatureListRow row) {
    // skip rows which have no isotope pattern and no MS/MS spectrum
    for (Feature f : row.getFeatures()) {
      if (f.getFeatureStatus() == FeatureStatus.DETECTED) {
        if ((f.getIsotopePattern() != null && f.getIsotopePattern().getNumberOfDataPoints() > 1)
            || f.getMostIntenseFragmentScan() != null)
          return false;
      }
    }
    return true;
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, MergedSpectrum mergedSpectrum) throws IOException {
    writeHeader(writer, row, raw, polarity, msType, row.getID(), Arrays
        .stream(mergedSpectrum.origins).map(RawDataFile::getName).collect(Collectors.toList()));
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
      char polarity, MsType msType, Scan scanNumber) throws IOException {
    writeHeader(writer, row, raw, polarity, msType, scanNumber.getScanNumber(), null);
  }

  private void writeHeader(BufferedWriter writer, FeatureListRow row, RawDataFile raw,
      char polarity, MsType msType, Integer scanNumber, List<String> sources) throws IOException {
    final Feature feature = row.getFeature(raw);
    writer.write("BEGIN IONS");
    writer.newLine();
    writer.write("FEATURE_ID=");
    writer.write(String.valueOf(row.getID()));
    writer.newLine();
    writer.write("PEPMASS=");
    writer.write(String.valueOf(row.getBestFeature().getMZ()));
    writer.newLine();
    writer.write("CHARGE=");
    if (polarity == '-')
      writer.write("-");
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
      for (RawDataFile f : raws)
        set.add(f.getName());
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

  private static enum MsType {
    MS, MSMS, CORRELATED
  }

}
