/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.inference.TTestImpl;


public class HeatMapTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RSessionWrapper rSession;
	private String errorMsg;
	private boolean userCanceled;

	private final MZmineProject project;
	private String outputType;
	private boolean log, rcontrol, scale, plegend, area, onlyIdentified;
	private int height, width, columnMargin, rowMargin, starSize;
	private File outputFile;
	private double[][] newPeakList;
	private String[] rowNames, colNames;
	private String[][] pValueMatrix;
	private double finishedPercentage = 0.0f;
	private UserParameter<?, ?> selectedParameter;
	private Object referenceGroup;
	private PeakList peakList;

	public HeatMapTask(MZmineProject project, PeakList peakList,
			ParameterSet parameters) {

		this.project = project;
		this.peakList = peakList;

		// Parameters
		outputFile = parameters.getParameter(HeatMapParameters.fileName)
				.getValue();
		outputType = parameters.getParameter(
				HeatMapParameters.fileTypeSelection).getValue();
		selectedParameter = parameters.getParameter(
				HeatMapParameters.selectionData).getValue();
		referenceGroup = parameters.getParameter(
				HeatMapParameters.referenceGroup).getValue();
		area = parameters.getParameter(HeatMapParameters.usePeakArea)
				.getValue();
		onlyIdentified = parameters.getParameter(
				HeatMapParameters.useIdenfiedRows).getValue();

		log = parameters.getParameter(HeatMapParameters.log).getValue();
		scale = parameters.getParameter(HeatMapParameters.scale).getValue();
		rcontrol = parameters
				.getParameter(HeatMapParameters.showControlSamples).getValue();
		plegend = parameters.getParameter(HeatMapParameters.plegend).getValue();

		height = parameters.getParameter(HeatMapParameters.height).getValue();
		width = parameters.getParameter(HeatMapParameters.width).getValue();
		columnMargin = parameters.getParameter(HeatMapParameters.columnMargin)
				.getValue();
		rowMargin = parameters.getParameter(HeatMapParameters.rowMargin)
				.getValue();
		starSize = parameters.getParameter(HeatMapParameters.star).getValue();

		this.userCanceled = false;
	}

	public String getTaskDescription() {
		return "Heat map... ";
	}

	public double getFinishedPercentage() {
		return finishedPercentage;
	}

	@Override
	public void cancel() {
		this.userCanceled = true;

		super.cancel();

		// Turn off R instance, if already existing.
		try {
			if (this.rSession != null) this.rSession.close(true);
		}
		catch (RSessionWrapperException e) {
			// Silent, always...
		}
	}

	public void run() {
		errorMsg = null;

		setStatus(TaskStatus.PROCESSING);

		logger.info("Heat map plot");

		if (plegend) {
			newPeakList = groupingDataset(selectedParameter,
					referenceGroup.toString());
		} else {
			newPeakList = modifySimpleDataset(selectedParameter,
					referenceGroup.toString());
		}

		if (newPeakList.length == 0 || newPeakList[0].length == 0) {
			setStatus(TaskStatus.ERROR);
			setErrorMessage("The data for heat map is empty.");
			return;
		}

		try {

			// Load gplots library
			String[] reqPackages = { "gplots" };
			this.rSession = new RSessionWrapper("HeatMap analysis module", reqPackages, null);
			this.rSession.open();	

			finishedPercentage = 0.3f;


			if (outputType.contains("png")) {
				if (height < 500 || width < 500) {

					setStatus(TaskStatus.ERROR);
					setErrorMessage("Figure height or width is too small. Minimun height and width is 500.");
					return;
				}
			}

			this.rSession.eval("dataset<- matrix(\"\",nrow ="
					+ newPeakList[0].length + ",ncol=" + newPeakList.length
					+ ")");

			if (plegend) {
				this.rSession.eval("stars<- matrix(\"\",nrow ="
						+ newPeakList[0].length + ",ncol="
						+ newPeakList.length + ")");
			}

			// assing the values to the matrix
			for (int row = 0; row < newPeakList[0].length; row++) {

				for (int column = 0; column < newPeakList.length; column++) {

					int r = row + 1;
					int c = column + 1;

					double value = newPeakList[column][row];

					if (plegend) {
						String pValue = pValueMatrix[column][row];
						this.rSession.eval("stars[" + r + "," + c + "] = \""
								+ pValue + "\"");
					}

					if (!Double.isInfinite(value) && !Double.isNaN(value)) {

						this.rSession.eval("dataset[" + r + "," + c + "] = "
								+ value);
					} else {

						this.rSession.eval("dataset[" + r + "," + c + "] = NA");
					}
				}
			}
			finishedPercentage = 0.4f;

			this.rSession.eval("dataset <- apply(dataset, 2, as.numeric)");

			// Assign row names to the data set
			this.rSession.assign("rowNames", rowNames);
			this.rSession.eval("rownames(dataset)<-rowNames");

			// Assign column names to the data set
			this.rSession.assign("colNames", colNames);
			this.rSession.eval("colnames(dataset)<-colNames");

			finishedPercentage = 0.5f;

			// Remove the rows with too many NA's. The distances between
			// rows can't be calculated if the rows don't have
			// at least one sample in common.
			this.rSession.eval(" d <- as.matrix(dist(dataset))");
			this.rSession.eval("d[upper.tri(d)] <- 0");
			this.rSession.eval("dataset <- dataset[-na.action(na.omit(d)),]");

			finishedPercentage = 0.8f;

			String marginParameter = "margins = c(" + columnMargin + ","
					+ rowMargin + ")";
			this.rSession.eval(
					"br<-c(seq(from=min(dataset,na.rm=T),to=0,length.out=256),seq(from=0,to=max(dataset,na.rm=T),length.out=256))",	false);

			// Possible output file types
			if (outputType.contains("pdf")) {

				this.rSession.eval("pdf(\"" + outputFile + "\", height=" + height
						+ ", width=" + width + ")");
			} else if (outputType.contains("fig")) {

				this.rSession.eval("xfig(\"" + outputFile + "\", height="
						+ height + ", width=" + width
						+ ", horizontal = FALSE, pointsize = 12)");
			} else if (outputType.contains("svg")) {

				// Load RSvgDevice library
				this.rSession.loadPackage("RSvgDevice");

				this.rSession.eval("devSVG(\"" + outputFile + "\", height="
						+ height + ", width=" + width + ")");
			} else if (outputType.contains("png")) {

				this.rSession.eval("png(\"" + outputFile + "\", height=" + height
						+ ", width=" + width + ")");
			}

			if (plegend) {

				this.rSession.eval(
						"heatmap.2(dataset,"
								+ marginParameter
								+ ", trace=\"none\", col=bluered(length(br)-1), breaks=br, cellnote=stars, notecol=\"black\", notecex="
								+ starSize + ", na.color=\"grey\")", false);
			} else {

				this.rSession.eval(
						"heatmap.2(dataset,"
								+ marginParameter
								+ ", trace=\"none\", col=bluered(length(br)-1), breaks=br, na.color=\"grey\")", false);
			}

			this.rSession.eval("dev.off()", false);
			finishedPercentage = 1.0f;


			// Turn off R instance, once task ended gracefully.
			if (!this.userCanceled) this.rSession.close(false);

		} 
		catch (RSessionWrapperException e) {
			if (!this.userCanceled) {
				errorMsg = "'R computing error' during heatmap generation. \n" + e.getMessage();
			}
		}
		catch (Exception e) {
			if (!this.userCanceled) {
				errorMsg = "'Unknown error' during heatmap generation. \n" + e.getMessage();
			}
		}

		// Turn off R instance, once task ended UNgracefully.
		try {
			if (!this.userCanceled) this.rSession.close(this.userCanceled);
		}
		catch (RSessionWrapperException e) {
			if (!this.userCanceled) {
				// Do not override potential previous error message.
				if (errorMsg == null) {
					errorMsg = e.getMessage();
				}
			} else {
				// User canceled: Silent.
			}
		}

		// Report error.
		if (errorMsg != null) {
			setErrorMessage(errorMsg);
			setStatus(TaskStatus.ERROR);				
		} else  {
			setStatus(TaskStatus.FINISHED);
		}
	}

	private double[][] modifySimpleDataset(UserParameter<?, ?> selectedParameter,
			String referenceGroup) {

		// Collect all data files
		Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
		allDataFiles.addAll(Arrays.asList(peakList.getRawDataFiles()));

		// Determine the reference group and non reference group (the rest of
		// the samples) for raw data files
		List<RawDataFile> referenceDataFiles = new ArrayList<RawDataFile>();
		List<RawDataFile> nonReferenceDataFiles = new ArrayList<RawDataFile>();

		for (RawDataFile rawDataFile : allDataFiles) {

			Object paramValue = project.getParameterValue(selectedParameter,
					rawDataFile);

			if (paramValue.equals(referenceGroup)) {

				referenceDataFiles.add(rawDataFile);
			} else {

				nonReferenceDataFiles.add(rawDataFile);
			}
		}

		int numRows = 0;
		for (int row = 0; row < peakList.getNumberOfRows(); row++) {

			if (!onlyIdentified
					|| (onlyIdentified && peakList.getRow(row)
							.getPeakIdentities().length > 0)) {
				numRows++;
			}
		}

		// Create a new aligned peak list with all the samples if the reference
		// group has to be shown or with only
		// the non reference group if not.
		double[][] dataMatrix;
		if (rcontrol) {
			dataMatrix = new double[allDataFiles.size()][numRows];
		} else {
			dataMatrix = new double[nonReferenceDataFiles.size()][numRows];
		}

		// Data files that should be in the heat map
		List<RawDataFile> shownDataFiles = null;
		if (rcontrol) {
			shownDataFiles = allDataFiles;
		} else {
			shownDataFiles = nonReferenceDataFiles;
		}

		for (int row = 0, rowIndex = 0; row < peakList.getNumberOfRows(); row++) {
			PeakListRow rowPeak = peakList.getRow(row);
			if (!onlyIdentified
					|| (onlyIdentified && rowPeak.getPeakIdentities().length > 0)) {

				// Average area or height of the reference group
				double referenceAverage = 0;
				int referencePeakCount = 0;
				for (int column = 0; column < referenceDataFiles.size(); column++) {

					if (rowPeak.getPeak(referenceDataFiles.get(column)) != null) {

						if (area) {

							referenceAverage += rowPeak.getPeak(
									referenceDataFiles.get(column)).getArea();
						} else {

							referenceAverage += rowPeak.getPeak(
									referenceDataFiles.get(column)).getHeight();
						}
						referencePeakCount++;
					}
				}
				if (referencePeakCount > 0) {

					referenceAverage /= referencePeakCount;
				}

				// Divide the area or height of each peak by the average of the
				// area or height of the reference peaks in each row
				for (int column = 0; column < shownDataFiles.size(); column++) {
					double value = Double.NaN;
					if (rowPeak.getPeak(shownDataFiles.get(column)) != null) {

						Feature peak = rowPeak.getPeak(shownDataFiles
								.get(column));
						if (area) {

							value = peak.getArea() / referenceAverage;
						} else {

							value = peak.getHeight() / referenceAverage;
						}
						if (log) {

							value = Math.log(value);
						}
					}

					dataMatrix[column][rowIndex] = value;
				}
				rowIndex++;
			}
		}

		// Scale the data dividing the peak area/height by the standard
		// deviation of each column
		if (scale) {
			scale(dataMatrix);
		}

		// Create two arrays: row and column names
		rowNames = new String[dataMatrix[0].length];
		colNames = new String[shownDataFiles.size()];

		for (int column = 0; column < shownDataFiles.size(); column++) {

			colNames[column] = shownDataFiles.get(column).getName();
		}
		for (int row = 0, rowIndex = 0; row < peakList.getNumberOfRows(); row++) {
			if (!onlyIdentified
					|| (onlyIdentified && peakList.getRow(row)
							.getPeakIdentities().length > 0)) {
				if (peakList.getRow(row).getPeakIdentities() != null
						&& peakList.getRow(row).getPeakIdentities().length > 0) {

					rowNames[rowIndex++] = peakList.getRow(row)
							.getPreferredPeakIdentity().getName();
				} else {

					rowNames[rowIndex++] = "Unknown";
				}
			}
		}

		return dataMatrix;
	}

	private void scale(double[][] peakList) {
		DescriptiveStatistics stdDevStats = new DescriptiveStatistics();

		for (int columns = 0; columns < peakList.length; columns++) {
			stdDevStats.clear();
			for (int row = 0; row < peakList[columns].length; row++) {
				if (!Double.isInfinite(peakList[columns][row])
						&& !Double.isNaN(peakList[columns][row])) {
					stdDevStats.addValue(peakList[columns][row]);
				}
			}

			double stdDev = stdDevStats.getStandardDeviation();

			for (int row = 0; row < peakList[columns].length; row++) {
				if (stdDev != 0) {
					peakList[columns][row] = peakList[columns][row] / stdDev;
				}
			}
		}
	}

	private double[][] groupingDataset(UserParameter<?, ?> selectedParameter,
			String referenceGroup) {
		// Collect all data files
		Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
		DescriptiveStatistics meanControlStats = new DescriptiveStatistics();
		DescriptiveStatistics meanGroupStats = new DescriptiveStatistics();
		allDataFiles.addAll(Arrays.asList(peakList.getRawDataFiles()));

		// Determine the reference group and non reference group (the rest of
		// the samples) for raw data files
		List<RawDataFile> referenceDataFiles = new ArrayList<RawDataFile>();
		List<RawDataFile> nonReferenceDataFiles = new ArrayList<RawDataFile>();

		List<String> groups = new ArrayList<String>();

		for (RawDataFile rawDataFile : allDataFiles) {

			Object paramValue = project.getParameterValue(selectedParameter,
					rawDataFile);
			if (!groups.contains(String.valueOf(paramValue))) {
				groups.add(String.valueOf(paramValue));
			}
			if (String.valueOf(paramValue).equals(referenceGroup)) {

				referenceDataFiles.add(rawDataFile);
			} else {

				nonReferenceDataFiles.add(rawDataFile);
			}
		}

		int numRows = 0;
		for (int row = 0; row < peakList.getNumberOfRows(); row++) {

			if (!onlyIdentified
					|| (onlyIdentified && peakList.getRow(row)
							.getPeakIdentities().length > 0)) {
				numRows++;
			}
		}

		// Create a new aligned peak list with all the samples if the reference
		// group has to be shown or with only
		// the non reference group if not.
		double[][] dataMatrix = new double[groups.size() - 1][numRows];
		pValueMatrix = new String[groups.size() - 1][numRows];

		// data files that should be in the heat map
		List<RawDataFile> shownDataFiles = nonReferenceDataFiles;

		for (int row = 0, rowIndex = 0; row < peakList.getNumberOfRows(); row++) {
			PeakListRow rowPeak = peakList.getRow(row);
			if (!onlyIdentified
					|| (onlyIdentified && rowPeak.getPeakIdentities().length > 0)) {
				// Average area or height of the reference group
				meanControlStats.clear();
				for (int column = 0; column < referenceDataFiles.size(); column++) {

					if (rowPeak.getPeak(referenceDataFiles.get(column)) != null) {

						if (area) {

							meanControlStats.addValue(rowPeak.getPeak(
									referenceDataFiles.get(column)).getArea());
						} else {

							meanControlStats
							.addValue(rowPeak.getPeak(
									referenceDataFiles.get(column))
									.getHeight());
						}

					}
				}

				// Divide the area or height of each peak by the average of the
				// area or height of the reference peaks in each row
				int columnIndex = 0;
				for (int column = 0; column < groups.size(); column++) {
					String group = groups.get(column);
					meanGroupStats.clear();
					if (!group.equals(referenceGroup)) {

						for (int dataColumn = 0; dataColumn < shownDataFiles
								.size(); dataColumn++) {

							Object paramValue = project.getParameterValue(
									selectedParameter,
									shownDataFiles.get(dataColumn));
							if (rowPeak.getPeak(shownDataFiles.get(dataColumn)) != null
									&& String.valueOf(paramValue).equals(group)) {

								Feature peak = rowPeak.getPeak(shownDataFiles
										.get(dataColumn));

								if (!Double.isInfinite(peak.getArea())
										&& !Double.isNaN(peak.getArea())) {

									if (area) {

										meanGroupStats.addValue(peak.getArea());
									} else {

										meanGroupStats.addValue(peak
												.getHeight());
									}
								}

							}
						}

						double value = meanGroupStats.getMean()
								/ meanControlStats.getMean();
						if (meanGroupStats.getN() > 1
								&& meanControlStats.getN() > 1) {
							pValueMatrix[columnIndex][rowIndex] = this
									.getPvalue(meanGroupStats, meanControlStats);
						} else {
							pValueMatrix[columnIndex][rowIndex] = "";
						}

						if (log) {

							value = Math.log(value);
						}
						dataMatrix[columnIndex++][rowIndex] = value;
					}
				}
				rowIndex++;
			}
		}

		// Scale the data dividing the peak area/height by the standard
		// deviation of each column
		if (scale) {
			scale(dataMatrix);
		}

		// Create two arrays: row and column names
		rowNames = new String[dataMatrix[0].length];
		colNames = new String[groups.size() - 1];

		int columnIndex = 0;
		for (String group : groups) {

			if (!group.equals(referenceGroup)) {

				colNames[columnIndex++] = group;
			}
		}
		for (int row = 0, rowIndex = 0; row < peakList.getNumberOfRows(); row++) {
			if (!onlyIdentified
					|| (onlyIdentified && peakList.getRow(row)
							.getPeakIdentities().length > 0)) {
				if (peakList.getRow(row).getPeakIdentities() != null
						&& peakList.getRow(row).getPeakIdentities().length > 0) {

					rowNames[rowIndex++] = peakList.getRow(row)
							.getPreferredPeakIdentity().getName();
				} else {

					rowNames[rowIndex++] = "Unknown";
				}
			}
		}

		return dataMatrix;
	}

	private String getPvalue(DescriptiveStatistics group1,
			DescriptiveStatistics group2) {
		TTestImpl ttest = new TTestImpl();
		String sig = "";
		try {
			double pValue = ttest.tTest(group1, group2);
			if (pValue < 0.05) {
				sig = "*";
			}
			if (pValue < 0.01) {
				sig = "**";
			}
			if (pValue < 0.001) {
				sig = "***";
			}

		} catch (IllegalArgumentException ex) {
			sig = "-";

		} catch (MathException ex) {
			sig = "-";
		}
		return sig;
	}
}
