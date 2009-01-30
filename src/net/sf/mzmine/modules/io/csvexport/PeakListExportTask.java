/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.io.csvexport;

import java.io.FileWriter;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

class PeakListExportTask implements Task {

	private PeakList peakList;
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int processedRows, totalRows;

	// parameter values
	private String fileName, fieldSeparator;
	private ExportRowElement[] elements;

	PeakListExportTask(PeakList peakList, PeakListExportParameters parameters) {

		this.peakList = peakList;

		fileName = (String) parameters
				.getParameterValue(PeakListExportParameters.filename);
		fieldSeparator = (String) parameters
				.getParameterValue(PeakListExportParameters.fieldSeparator);

        elements = (ExportRowElement[]) parameters.getParameterValue(PeakListExportParameters.exportItemMultipleSelection);

	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0.0f;
		}
		return (double) processedRows / (double) totalRows;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Exporting peak list " + peakList + " to " + fileName;
	}

	public void run() {

		// Open file
		FileWriter writer;
		try {
			writer = new FileWriter(fileName);
		} catch (Exception e) {
			status = TaskStatus.ERROR;
			errorMessage = "Could not open file " + fileName + " for writing.";
			return;
		}

		// Get number of rows
		totalRows = peakList.getNumberOfRows();

        RawDataFile rawDataFiles[] = peakList.getRawDataFiles(); 
        
		// Buffer for writing
		StringBuffer line = new StringBuffer();

		// Write column headers
		int length = elements.length;
		String name;
		for (int i = 0; i < length; i++) {
			if (elements[i].isCommon()) {
				name = elements[i].getName();
				name = name.replace("Export ", "");
				line.append(name + fieldSeparator);
			}
		}

		for (int df = 0; df < peakList.getNumberOfRawDataFiles(); df++) {
			for (int i = 0; i < length; i++) {
				if (!elements[i].isCommon()) {
					name = elements[i].getName();
					name = name.replace("Export", rawDataFiles[df].getName());
					line.append(name + fieldSeparator);
				}
			}
		}

		line.append("\n");

		try {
			writer.write(line.toString());
		} catch (Exception e) {
			status = TaskStatus.ERROR;
			errorMessage = "Could not write to file " + fileName;
			return;
		}

		// Write data rows
		for (PeakListRow peakListRow : peakList.getRows()) {

			// Cancel?
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Reset the buffer
			line.setLength(0);

			// Write row data
			length = elements.length;
			for (int i = 0; i < length; i++) {
				if (elements[i].isCommon()) {
					switch (elements[i]) {
					case ROW_ID:
						line.append(peakListRow.getID() + fieldSeparator);
						break;
					case ROW_MZ:
						line
								.append(peakListRow.getAverageMZ()
										+ fieldSeparator);
						break;
					case ROW_RT:
						line.append((peakListRow.getAverageRT() / 60)
								+ fieldSeparator);
						break;
					case ROW_COMMENT:
						if (peakListRow.getComment() == null) {
							line.append(fieldSeparator);
						} else {
							line.append(peakListRow.getComment()
									+ fieldSeparator);
						}
						break;
					case ROW_NAME:
						if (peakListRow.getPreferredCompoundIdentity() == null) {
							line.append(fieldSeparator);
						} else {
                            
							line.append("\"" + peakListRow
									.getPreferredCompoundIdentity().getName()
									+ "\"" + fieldSeparator);
						}
						break;
					case ROW_ALL_NAME:
						if (peakListRow.getPreferredCompoundIdentity() == null) {
							line.append(fieldSeparator);
						} else {
							name = "";
							PeakIdentity[] compoundIdentities = peakListRow
									.getCompoundIdentities();
							for (PeakIdentity compoundIdentity : compoundIdentities) {
								name += compoundIdentity.getName() + " // ";
							}
							line.append("\"" + name + "\"" + fieldSeparator);
						}
						break;
					case ROW_FORMULA:
						if (peakListRow.getPreferredCompoundIdentity() == null) {
							line.append(fieldSeparator);
						} else {
							line.append(peakListRow
									.getPreferredCompoundIdentity()
									.getCompoundFormula()
									+ fieldSeparator);
						}
						break;
					case ROW_PEAK_NUMBER:
						int numDetected = 0;
						for (ChromatographicPeak p : peakListRow.getPeaks()) {
							if (p.getPeakStatus() == PeakStatus.DETECTED) {
								numDetected++;
							}
						}
						line.append(numDetected + fieldSeparator);
						break;
					}
				}
			}

			for (RawDataFile dataFile : rawDataFiles) {
				for (int i = 0; i < length; i++) {
					if (!elements[i].isCommon()) {
						ChromatographicPeak peak = peakListRow
								.getPeak(dataFile);
						if (peak != null) {
							switch (elements[i]) {
							case PEAK_STATUS:
								line.append(peak.getPeakStatus()
										+ fieldSeparator);
								break;
							case PEAK_MZ:
								line.append(peak.getMZ() + fieldSeparator);
								break;
							case PEAK_RT:
								line.append(peak.getRT() + fieldSeparator);
								break;
							case PEAK_HEIGHT:
								line.append(peak.getHeight() + fieldSeparator);
								break;
							case PEAK_AREA:
								line.append(peak.getArea() + fieldSeparator);
								break;
							}
						} else {
							line.append("N/A" + fieldSeparator);
						}
					}
				}
			}

			line.append("\n");

			try {
				writer.write(line.toString());
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "Could not write to file " + fileName;
				return;
			}

			processedRows++;

		}

		// Close file
		try {
			writer.close();
		} catch (Exception e) {
			status = TaskStatus.ERROR;
			errorMessage = "Could not close file " + fileName;
			return;
		}

		status = TaskStatus.FINISHED;

	}
}
