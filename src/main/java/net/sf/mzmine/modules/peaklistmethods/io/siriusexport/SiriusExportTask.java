/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang
 * at the Dorrestein Lab (University of California, San Diego).
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
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

public class SiriusExportTask extends AbstractTask {

    private final static String plNamePattern = "{}";
    protected final boolean includeMs1;
    private final PeakList[] peakLists;
	private final File fileName;
	// private final boolean fractionalMZ;
	private final String massListName;
    protected double progress, totalProgress;

	SiriusExportTask(ParameterSet parameters) {
		this.peakLists = parameters.getParameter(SiriusExportParameters.PEAK_LISTS).getValue().getMatchingPeakLists();

		this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();

		// this.fractionalMZ =
		// parameters.getParameter(SiriusExportParameters.FRACTIONAL_MZ)
		// .getValue();

		this.massListName = parameters.getParameter(SiriusExportParameters.MASS_LIST).getValue();
        this.includeMs1 = parameters.getParameter(SiriusExportParameters.INCLUDE_MSSCAN).getValue();
    }

	public double getFinishedPercentage() {
        return (totalProgress == 0 ? 0 : progress / totalProgress);
    }

	public String getTaskDescription() {
		return "Exporting peak list(s) " + Arrays.toString(peakLists) + " to MGF file(s)";
	}

	public void run() {
        this.progress = 0d;
        setStatus(TaskStatus.PROCESSING);

		// Shall export several files?
		boolean substitute = fileName.getPath().contains(plNamePattern);

        int counter = 0;
        for (PeakList l : peakLists)
            counter += l.getNumberOfRows();
        this.totalProgress = counter;

		// Process peak lists
		for (PeakList peakList : peakLists) {

			// Filename
			File curFile = fileName;
			if (substitute) {
				// Cleanup from illegal filename characters
				String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
				// Substitute
				String newFilename = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
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

    private void exportPeakList(PeakList peakList, BufferedWriter writer) throws IOException {

        final HashMap<String, int[]> fragmentScans = new HashMap<>();
        for (RawDataFile r : peakList.getRawDataFiles()) {
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

		for (PeakListRow row : peakList.getRows()) {
            if (isSkipRow(row))
                continue;
            // get row charge and polarity
            char polarity = 0;
            for (Feature f : row.getPeaks()) {
                char pol = f.getDataFile().getScan(f.getRepresentativeScanNumber()).getPolarity().asSingleChar().charAt(0);
                if (pol != polarity && polarity != 0) {
                    setErrorMessage("Joined features have different polarity. This is most likely a bug. If not, please separate them as individual features and/or write a feature request on github.");
                    setStatus(TaskStatus.ERROR);
                    return;
                } else {
                    polarity = pol;
                }
            }
            // write correlation spectrum
            writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.CORRELATED, -1);
            writeCorrelationSpectrum(writer, row.getBestPeak());
            // for each MS/MS write corresponding MS1 and MSMS spectrum
            for (Feature f : row.getPeaks()) {
                if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED && f.getMostIntenseFragmentScanNumber() >= 0) {
                    final int[] scanNumbers = f.getScanNumbers().clone();
                    Arrays.sort(scanNumbers);
                    int[] fs = fragmentScans.get(f.getDataFile().getName());
                    int startWith = scanNumbers[0];
                    int j = Arrays.binarySearch(fs, startWith);
                    if (j < 0) j = (-j - 1);
                    for (int k = j; k < fs.length; ++k) {
                        final Scan scan = f.getDataFile().getScan(fs[k]);
                        if (scan.getMSLevel() > 1 && Math.abs(scan.getPrecursorMZ() - f.getMZ()) < 0.1) {
                            if (includeMs1) {
                                // find precursor scan
                                int prec = Arrays.binarySearch(scanNumbers, fs[k]);
                                if (prec < 0) prec = -prec - 1;
                                prec = Math.max(0, prec - 1);
                                for (; prec < scanNumbers.length && scanNumbers[prec] < fs[k]; ++prec) {
                                    final Scan precursorScan = f.getDataFile().getScan(scanNumbers[prec]);
                                    if (precursorScan.getMSLevel() == 1) {
                                        writeHeader(writer, row, f.getDataFile(), polarity, MsType.MS, precursorScan.getScanNumber());
                                        writeSpectrum(writer, massListName != null ? precursorScan.getMassList(massListName).getDataPoints() : precursorScan.getDataPoints());
                                    }
                                }
                            }
                            writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, scan.getScanNumber());
                            writeSpectrum(writer, massListName != null ? scan.getMassList(massListName).getDataPoints() : scan.getDataPoints());
                        }
                    }
                }
                ++progress;
            }
        }
    }

    private boolean isSkipRow(PeakListRow row) {
        // skip rows which have no isotope pattern and no MS/MS spectrum
        for (Feature f : row.getPeaks()) {
            if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED) {
                if ((f.getIsotopePattern() != null && f.getIsotopePattern().getDataPoints().length > 1) || f.getMostIntenseFragmentScanNumber() >= 0)
                    return false;
            }
        }
        return true;
    }

    ;

    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity, MsType msType, int scanNumber) throws IOException {
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
        writer.write(String.valueOf(Math.abs(row.getRowCharge())));
        writer.write(polarity);
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
        if (msType == MsType.CORRELATED) {
            RawDataFile[] raws = row.getRawDataFiles();
            writer.write(escape(raws[0].getName(), ";"));
            for (int i = 1; i < raws.length; ++i) {
                writer.write(";");
                writer.write(escape(raws[i].getName(), ";"));
            }
            writer.newLine();
        } else {
            writer.write(feature.getDataFile().getName());
            writer.newLine();
        }
        if (scanNumber >= 0) {
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
            writer.write(String.valueOf(dp.getIntensity()));
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