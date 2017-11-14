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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

public class GNPSExportTask extends AbstractTask {
	private final PeakList[] peakLists;
	private final File fileName;
	private final String plNamePattern = "{}";
	private int currentIndex = 0;

	private final String massListName;

	GNPSExportTask(ParameterSet parameters) {
		this.peakLists = parameters.getParameter(GNPSExportParameters.PEAK_LISTS)
				.getValue().getMatchingPeakLists();

		this.fileName = parameters.getParameter(GNPSExportParameters.FILENAME)
				.getValue();        

		this.massListName = parameters.getParameter(GNPSExportParameters.MASS_LIST)
				.getValue();
	}

	@Override
	public double getFinishedPercentage() {
		if (peakLists.length == 0)
			return 1;
		else
			return currentIndex / peakLists.length;
	}

	@Override
	public void run() {
		setStatus(TaskStatus.PROCESSING);

		// Shall export several files?
		boolean substitute = fileName.getPath().contains(plNamePattern);

		// Process peak lists
		for (PeakList peakList: peakLists) {
			currentIndex++;

			// Filename
			File curFile = fileName;
			if (substitute) {
				// Cleanup from illegal filename characters
				String cleanPlName = peakList.getName().replaceAll(
						"[^a-zA-Z0-9.-]", "_");
				// Substitute
				String newFilename = fileName.getPath().replaceAll(
						Pattern.quote(plNamePattern), cleanPlName);
				curFile = new File(newFilename);
			}

			// Open file
			FileWriter writer;
			try {
				writer = new FileWriter(curFile);
			} catch (Exception e) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Could not open file " + curFile 
						+ " for writing.");
				return;
			}

			try {
				export(peakList, writer, curFile);
			} catch (IOException e) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Error while writing into file " + curFile +
						": " + e.getMessage());
				return;
			}

			// Cancel?
			if (isCanceled()) {
				return;
			}

			// Close file
			try {
				writer.close();
			} catch (Exception e) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Could not close file " + curFile);
				return;
			}

			// If peak list substitution pattern wasn't found, 
			// treat one peak list only
			if (!substitute)
				break;
		}

		if (getStatus() == TaskStatus.PROCESSING)
			setStatus(TaskStatus.FINISHED);
	}

	private void export(PeakList peakList, FileWriter writer, File curFile)
			throws IOException
	{
		final String newLine = System.lineSeparator();

		for (PeakListRow row : peakList.getRows()) {
			String rowID = Integer.toString(row.getID());

			String retTimeInSeconds = Double.toString(Math.round(row.getAverageRT() * 60 * 100)/100.);
			// Get the MS/MS scan number
			Feature bestPeak = row.getBestPeak();
			if(bestPeak == null) continue;
			int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
			if (rowID != null) {
				PeakListRow copyRow = copyPeakRow(row);
				// Best peak always exists, because peak list row has at least one peak
				bestPeak = copyRow.getBestPeak();

				// Get the MS/MS scan number

				msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
				while (msmsScanNumber < 1) {
					copyRow.removePeak(bestPeak.getDataFile());
					if(copyRow.getPeaks().length == 0)
						break;
						
					bestPeak = copyRow.getBestPeak();
					msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
			    }
			}
			if (msmsScanNumber >= 1) {
				// MS/MS scan must exist, because msmsScanNumber was > 0
				Scan msmsScan = bestPeak.getDataFile().getScan(msmsScanNumber);

				MassList massList = msmsScan.getMassList(massListName);

				if (massList == null) {
					MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
							"There is no mass list called " + massListName + " for MS/MS scan #" + msmsScanNumber
							+ " (" + bestPeak.getDataFile() + ")");
					return;
				}

				writer.write("BEGIN IONS" + newLine);

				if (rowID != null)
					writer.write("FEATURE_ID=" + rowID + newLine);

				String mass = Double.toString(Math.round(row.getAverageMZ()*10000)/10000.);
				if (mass != null)
					writer.write("PEPMASS=" + mass + newLine);

				if (rowID != null) {
					writer.write("SCANS=" + rowID + newLine);
					writer.write("RTINSECONDS=" + retTimeInSeconds + newLine);
				}

				int msmsCharge = msmsScan.getPrecursorCharge();
				String msmsPolarity = msmsScan.getPolarity().asSingleChar();
				if (msmsPolarity.equals("0"))
					msmsPolarity = "";
				if (msmsCharge == 0) {
					msmsCharge = 1;
					msmsPolarity = "";
				}
				writer.write("CHARGE=" + msmsCharge + msmsPolarity + newLine);

				writer.write("MSLEVEL=2" + newLine);

				DataPoint peaks[] = massList.getDataPoints();
				for (DataPoint peak : peaks) {
					writer.write(peak.getMZ() + " " + peak.getIntensity() + newLine);
				}

				writer.write("END IONS" + newLine);
				writer.write(newLine);
			}
		}      

	}
	public String getTaskDescription() {
		return "Exporting GNPS of peak list(s) " 
				+ Arrays.toString(peakLists) + " to MGF file(s)";
	}

	/**
	 * Create a copy of a peak list row.
	 */
	private static PeakListRow copyPeakRow(final PeakListRow row) {
		// Copy the peak list row.
		final PeakListRow newRow = new SimplePeakListRow(row.getID());
		PeakUtils.copyPeakListRowProperties(row, newRow);

		// Copy the peaks.
		for (final Feature peak : row.getPeaks()) {


			final Feature newPeak = new SimpleFeature(peak);
			PeakUtils.copyPeakProperties(peak, newPeak);
			newRow.addPeak(peak.getDataFile(), newPeak);

		}

		return newRow;
	}

}
