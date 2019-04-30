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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.Range;
import gnu.trove.list.array.TIntArrayList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

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
    // private final boolean fractionalMZ;
    private final String massListName;
    protected long finishedRows, totalRows;
    protected final SiriusExportParameters.MERGE_MODE mergeMsMs;
    protected double cosineThreshold;
    protected double expectedPPM;

    private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();

    protected boolean mergeMasses;

    protected Range<Double> isolationWindow;

    protected MergeUtils mergeUtils = new MergeUtils();


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

        // this.fractionalMZ =
        // parameters.getParameter(SiriusExportParameters.FRACTIONAL_MZ)
        // .getValue();

        this.massListName = parameters.getParameter(SiriusExportParameters.MASS_LIST).getValue();
        this.mergeMsMs = parameters.getParameter(SiriusExportParameters.MERGE).getValue();

        this.cosineThreshold = parameters.getParameter(SiriusExportParameters.COSINE_PARAMETER).getValue();
        this.expectedPPM = parameters.getParameter(SiriusExportParameters.MASS_ACCURACY).getValue();

        this.mergeMasses = parameters.getParameter(SiriusExportParameters.AVERAGE_OVER_MASS).getValue();

        double offset = parameters.getParameter(SiriusExportParameters.isolationWindowOffset).getValue();
        double width = parameters.getParameter(SiriusExportParameters.isolationWindowWidth).getValue();
        this.isolationWindow = Range.closed(offset - width, offset + width);

        mergeUtils = new MergeUtils();
        mergeUtils.setCosineThreshold(this.cosineThreshold);
        mergeUtils.setExpectedPPM(this.expectedPPM);
        mergeUtils.setIsolationWindow(this.isolationWindow);
        mergeUtils.setMasslist(this.massListName);
        mergeUtils.setMergeMasses(this.mergeMasses);
        mergeUtils.setPeakRemovalThreshold(parameters.getParameter(SiriusExportParameters.PEAK_COUNT_PARAMETER).getValue());
        DEBUG_MODE = parameters.getParameter(SiriusExportParameters.DEBUG_INFORMATION).getValue();
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
            try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile, true))) {
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
            exportPeakListRow(row, bw, getFragmentScansNumbers(row.getRawDataFiles()));
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
                exportPeakListRow(row, bw, getFragmentScansNumbers(row.getRawDataFiles()));
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
        mergeUtils.prefillStatisticsWithTICExamples(copy.stream().limit(1000).map(r->r.getBestFragmentation()).filter(x->x!=null).collect(Collectors.toList()));
    }


    private void exportPeakList(PeakList peakList, BufferedWriter writer) throws IOException {

        final HashMap<String, int[]> fragmentScans = getFragmentScansNumbers(peakList.getRawDataFiles());

        for (PeakListRow row : peakList.getRows()) {
            exportPeakListRow(row, writer, fragmentScans);
            finishedRows++;
        }
    }

    private HashMap<String, int[]> getFragmentScansNumbers(RawDataFile[] rawDataFiles) {
        final HashMap<String, int[]> fragmentScans = new HashMap<>();
        for (RawDataFile r : rawDataFiles) {
            int[] scans = new int[0];
            for (int msLevel : r.getMSLevels()) {
                if (msLevel > 1) {
                    int[] concat = r.getScanNumbers(msLevel);
                    int offset = scans.length;
                    scans = Arrays.copyOf(scans, scans.length + concat.length);
                    System.arraycopy(concat, 0, scans, offset, concat.length);
                }
            }
            Arrays.sort(scans);
            fragmentScans.put(r.getName(), scans);
        }
        return fragmentScans;
    }

    private void exportPeakListRow(PeakListRow row, BufferedWriter writer,
                                   final HashMap<String, int[]> fragmentScans) throws IOException {
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
        // write correlation spectrum
        writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.CORRELATED, -1);
        writeCorrelationSpectrum(writer, row.getBestPeak());

        /*
         * first we check if we have at least one good fragment scan per
         */
        final List<MergeUtils.FragmentScan> allFragmentScans = new ArrayList<>();
        for (Feature f : row.getPeaks()) {
            final List<MergeUtils.FragmentScan> scans = mergeUtils.extractFragmentScansFor(row, f);
            if (scans.isEmpty())
                continue;
            if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_CONSECUTIVE_SCANS) {
                MergeUtils.MergedSpectrum dps = mergeUtils.mergeConsecutiveScans(f.getDataFile().getName() + ", m/z = " + f.getMZ() + ", rt = " + f.getRT(), f.getMZ(), scans, true);
                if (dps!=null && dps.data.length>0) {
                    writeHeader(writer, row, scans.get(0).origin, polarity, MsType.MSMS, dps);
                    writeSpectrum(writer, dps.data);
                }
            } else if (mergeMsMs == SiriusExportParameters.MERGE_MODE.NO_MERGE) {
                List<Integer> scanNumbers = new ArrayList<>();
                final List<DataPoint[]> spectra = mergeUtils.extractHighQualityScans(f.getMZ(), scans, scanNumbers);
                for (int k=0; k < spectra.size(); ++k) {
                    writeHeader(writer, row, scans.get(0).origin, polarity, MsType.MSMS, scanNumbers.get(k)); // TODO: add scan number
                    writeSpectrum(writer, spectra.get(k));
                }
            } else {
                allFragmentScans.addAll(scans);
            }
        }

        if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_OVER_SAMPLES ) {
            MergeUtils.MergedSpectrum dps = mergeUtils.mergeScansFromDifferentOrigins("m/z = " + row.getAverageMZ() + ", retention time = " + row.getAverageRT(), row.getAverageMZ(), allFragmentScans);
            if (dps.data.length > 0) {
                writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.MSMS, dps);
                writeSpectrum(writer, dps.data);
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
                             MsType msType, MergeUtils.MergedSpectrum mergedSpectrum) throws IOException {
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
        writer.write(String.valueOf(mergedSpectrum.scanIds.length));
        writer.write(" / ");
        writer.write(String.valueOf(mergedSpectrum.totalNumberOfScans()));
        writer.write(" (");
        writer.write(String.valueOf(mergedSpectrum.removedScansByLowQuality));
        writer.write(" removed due to low quality, ");
        writer.write(String.valueOf(mergedSpectrum.removedScansByLowCosine));
        writer.write(" removed due to low cosine).");
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
            final MergeUtils.MergeDataPoint mp = (dp instanceof MergeUtils.MergeDataPoint ? (MergeUtils.MergeDataPoint)dp : null);
            writer.write(String.valueOf(dp.getMZ()));
            writer.write(' ');
            writer.write(mp != null ? intensityForm.format(mp.getSumIntensity()) : intensityForm.format(dp.getIntensity()));
            if (DEBUG_MODE && mp!=null) {
                writer.write(' ');
                writer.write('#');
                writer.write(mp.getComment());
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

    private static enum MsType {
        MS, MSMS, CORRELATED
    }


}
