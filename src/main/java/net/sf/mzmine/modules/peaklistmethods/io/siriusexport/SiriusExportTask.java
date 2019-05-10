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

package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.msmsspectramerge.MergeMode;
import net.sf.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import net.sf.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import net.sf.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SiriusExportTask extends AbstractTask {

    private boolean DEBUG_MODE;

    private final static String plNamePattern = "{}";
    protected static final Comparator<DataPoint> CompareDataPointsByMz = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return Double.compare(o1.getMZ(), o2.getMZ());
        }
    };
    protected static final Comparator<DataPoint> CompareDataPointsByDecreasingInt = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return Double.compare(o2.getIntensity(), o1.getIntensity());
        }
    };
    private final PeakList[] peakLists;
    private final File fileName;
    private final String massListName;
    protected long finishedRows, totalRows;

    private final MsMsSpectraMergeModule mergeMethod;
    private final MsMsSpectraMergeParameters mergeParameters;

    private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();

    public double getFinishedPercentage() {
        return (totalRows == 0 ? 0.0 : (double) finishedRows / (double) totalRows);
    }

    public String getTaskDescription() {
        return "Exporting peak list(s) " + Arrays.toString(peakLists) + " to MGF file(s)";
    }

    SiriusExportTask(ParameterSet parameters) {
        this.peakLists = parameters.getParameter(SiriusExportParameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists();
        this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();
        this.massListName = parameters.getParameter(SiriusExportParameters.MASS_LIST).getValue();
        OptionalModuleParameter<MsMsSpectraMergeParameters> parameter = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER);
        mergeParameters = parameter.getValue().booleanValue() ? parameter.getEmbeddedParameters() : null;
        mergeMethod = mergeParameters==null ? null : new MsMsSpectraMergeModule(mergeParameters);
    }

    public void run() {
        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        for (PeakList l : peakLists) {
            this.totalRows += l.getNumberOfRows();
            prefillStatistics(l.getRows());
        }

        // Process peak lists
        for (PeakList peakList : peakLists) {

            // Filename
            File curFile = fileName;
            if (substitute) {
                // Cleanup from illegal filename characters
                String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
                // Substitute
                String newFilename =
                        fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                curFile = new File(newFilename);
            }

            // Open file
            try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile))) {
                exportPeakList(peakList, bw);
            } catch (IOException e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not open file " + curFile + " for writing.");
            }

            // If peak list substitution pattern wasn't found,
            // treat one peak list only
            if (!substitute)
                break;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }

    public void runSingleRow(PeakListRow row) {
        setStatus(TaskStatus.PROCESSING);
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            exportPeakListRow(row, bw);
        } catch (IOException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not open file " + fileName + " for writing.");
        }
        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }


    public void runSingleRows(PeakListRow[] rows) {
        setStatus(TaskStatus.PROCESSING);
        // prefill statistics
        prefillStatistics(rows);
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            for (PeakListRow row : rows)
                exportPeakListRow(row, bw);
        } catch (IOException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not open file " + fileName + " for writing.");
        }
        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }

    private void prefillStatistics(PeakListRow[] rows) {
        ArrayList<PeakListRow> copy = new ArrayList<>(Arrays.asList(rows));
        Collections.shuffle(copy);
    }


    private void exportPeakList(PeakList peakList, BufferedWriter writer) throws IOException {
        for (PeakListRow row : peakList.getRows()) {
            exportPeakListRow(row, writer);
            finishedRows++;
        }
    }

    private void exportPeakListRow(PeakListRow row, BufferedWriter writer) throws IOException {
        if (isSkipRow(row))
            return;
        // get row charge and polarity
        char polarity = 0;
        for (Feature f : row.getPeaks()) {
            char pol = f.getDataFile().getScan(f.getRepresentativeScanNumber()).getPolarity()
                    .asSingleChar().charAt(0);
            if (pol != polarity && polarity != 0) {
                setErrorMessage(
                        "Joined features have different polarity. This is most likely a bug. If not, please separate them as individual features and/or write a feature request on github.");
                setStatus(TaskStatus.ERROR);
                return;
            } else {
                polarity = pol;
            }
        }

        MergeMode mergeMode = mergeParameters==null ? null : mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
        if ((mergeMode != MergeMode.ACROSS_SAMPLES)) {
            for (Feature f : row.getPeaks()) {
                if (f.getFeatureStatus()== Feature.FeatureStatus.DETECTED && f.getMostIntenseFragmentScanNumber() >= 0) {
                    // write correlation spectrum
                    writeHeader(writer, row, f.getDataFile(), polarity, MsType.CORRELATED, -1);
                    writeCorrelationSpectrum(writer, f);
                    if (mergeMode==MergeMode.CONSECUTIVE_SCANS) {
                        // merge MS/MS
                        List<MergedSpectrum> spectra = new MsMsSpectraMergeModule(mergeParameters).mergeConsecutiveScans(f,massListName);
                        for (MergedSpectrum spectrum : spectra) {
                            writeHeader(writer,row,f.getDataFile(),polarity,MsType.MSMS,spectrum.filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue()));
                            writeSpectrum(writer, spectrum.data);
                        }
                    } else {
                        MergedSpectrum spectrum = mergeMethod.mergeFromSameSample(f,massListName).filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue());
                        if (spectrum.data.length>0) {
                            writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, spectrum);
                            writeSpectrum(writer, spectrum.data);
                        }
                    }
                }
            }
        } else {
            // write correlation spectrum
            writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.CORRELATED, -1);
            writeCorrelationSpectrum(writer, row.getBestPeak());
            // merge everything into one
            MergedSpectrum spectrum = mergeMethod.mergeAcrossSamples(row,massListName).filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue());
            if (spectrum.data.length>0) {
                writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.MSMS, spectrum);
                writeSpectrum(writer, spectrum.data);
            }
        }
    }

    private boolean isSkipRow(PeakListRow row) {
        // skip rows which have no isotope pattern and no MS/MS spectrum
        for (Feature f : row.getPeaks()) {
            if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED) {
                if ((f.getIsotopePattern() != null && f.getIsotopePattern().getDataPoints().length > 1)
                        || f.getMostIntenseFragmentScanNumber() >= 0)
                    return false;
            }
        }
        return true;
    }

    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
                             MsType msType, MergedSpectrum mergedSpectrum) throws IOException {
        writeHeader(writer, row, raw, polarity, msType, row.getID(), Arrays.stream(mergedSpectrum.origins).map(RawDataFile::getName).collect(Collectors.toList()));
        // add additional fields
        writer.write("MERGED_SCANS=");
        writer.write(String.valueOf(mergedSpectrum.scanIds[0]));
        for (int k=1; k < mergedSpectrum.scanIds.length; ++k) {
            writer.write(',');
            writer.write(String.valueOf(mergedSpectrum.scanIds[k]));
        }
        writer.newLine();
        writer.write("MERGED_STATS=");
        writer.write(mergedSpectrum.getMergeStatsDescription());
        writer.newLine();
    }

    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
                             MsType msType, Integer scanNumber) throws IOException {
        writeHeader(writer, row, raw, polarity, msType, scanNumber, null);
    }

    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
                             MsType msType, Integer scanNumber, List<String> sources) throws IOException {
        final Feature feature = row.getPeak(raw);
        writer.write("BEGIN IONS");
        writer.newLine();
        writer.write("FEATURE_ID=");
        writer.write(String.valueOf(row.getID()));
        writer.newLine();
        writer.write("PEPMASS=");
        writer.write(String.valueOf(row.getBestPeak().getMZ()));
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
            RawDataFile[] raws = row.getRawDataFiles();
            final Set<String> set = new HashSet<>();
            for (RawDataFile f : raws) set.add(f.getName());
            final String[] uniqSources = set.toArray(new String[0]);
            writer.write(escape(uniqSources[0], ";"));
            for (int i = 1; i < uniqSources.length; ++i) {
                writer.write(";");
                writer.write(escape(uniqSources[i], ";"));
            }
            writer.newLine();
        } else {
            writer.write(feature.getDataFile().getName());
            writer.newLine();
        }
        if (scanNumber != null) {
            writer.write("SCANS=");
            writer.write(String.valueOf(scanNumber));
            writer.newLine();
        }
    }

    private void writeCorrelationSpectrum(BufferedWriter writer, Feature feature) throws IOException {
        if (feature.getIsotopePattern() != null) {
            writeSpectrum(writer, feature.getIsotopePattern().getDataPoints());
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

    private void writeSpectrum(BufferedWriter writer, DataPoint[] dps) throws IOException {
        for (DataPoint dp : dps) {
            writer.write(String.valueOf(dp.getMZ()));
            writer.write(' ');
            writer.write(intensityForm.format(dp.getIntensity()));
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
