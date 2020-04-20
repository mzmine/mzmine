/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.combinedmodule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javafx.application.Platform;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

public class CombinedModuleDataset extends AbstractXYDataset implements Task, XYToolTipGenerator {

		private RawDataFile rawDataFile;
		private Range<Double> totalRTRange, totalMZRange;
		private CombinedModuleVisualizerWindowController visualizer;
		private TaskStatus status = TaskStatus.WAITING;
		private int processedScans, scanNumbers[];
		private List<Double> targetedMZ_List;
		private List<Double> targetedNF_List;
		private Double basePeakPercent;
		private MZTolerance mzDifference;
		private HashMap<Integer, Vector<CombinedModuleDataPoint>> dataSeries;
		int totalScans;
		private static int RAW_LEVEL = 0;
		private static int PRECURSOR_LEVEL = 1;
		private static int NEUTRALLOSS_LEVEL = 2;
		private AxisType xAxisType;

		public CombinedModuleDataset(RawDataFile dataFile, Range<Double> rtRange, Range<Double> mzRange,
				CombinedModuleVisualizerWindowController visualizer) {
				this.rawDataFile = dataFile;
				this.totalMZRange = mzRange;
				this.totalRTRange = rtRange;
				this.visualizer = visualizer;

				targetedMZ_List = new ArrayList<Double>();
				targetedNF_List = new ArrayList<Double>();
				basePeakPercent = 0.0;
				scanNumbers = rawDataFile.getScanNumbers(2, rtRange);
				totalScans = scanNumbers.length;
				dataSeries = new HashMap<Integer, Vector<CombinedModuleDataPoint>>();
				dataSeries.put(RAW_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));
				dataSeries.put(PRECURSOR_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));
				dataSeries.put(NEUTRALLOSS_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));

				MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
		}

		@Override
		public String getTaskDescription() {
				return "Updating MS/MS visualizer of " + rawDataFile;
		}

		@Override
		public double getFinishedPercentage() {
				if (totalScans == 0) {
						return 0;
				} else {
						return ((double) processedScans / totalScans);
				}
		}

		@Override
		public TaskStatus getStatus() {
				return status;
		}

		@Override
		public String getErrorMessage() {
				return null;
		}

		@Override
		public TaskPriority getTaskPriority() {
				return TaskPriority.NORMAL;
		}

		@Override
		public void cancel() {
				status = TaskStatus.CANCELED;
		}

		public void setStatus(TaskStatus newStatus) {
				this.status = newStatus;
		}

		@Override
		public void run() {

				setStatus(TaskStatus.PROCESSING);
				processedScans = 0;

				// dataList that will contain output m/z values, RT, and scan number for
				// ID, ##for use in
				// targeted feature detection
				List<String> dataList = new ArrayList<String>();

				// in house generated list, used to output each precursor/product ion
				// m/z for plotting in R
				List<String> dataListVisual = new ArrayList<String>();

				for (int scanNumber : scanNumbers) {

						// Cancel?
						if (status == TaskStatus.CANCELED) {
								return;
						}

						Scan scan = rawDataFile.getScan(scanNumber);

						// check parent m/z
						if (!totalMZRange.contains(scan.getPrecursorMZ())) {
								continue;
						}

						// get m/z and intensity values
						DataPoint[] scanDataPoints = scan.getDataPoints();

						// skip empty scans
						if (scan.getHighestDataPoint() == null) {
								processedScans++;
								continue;
						}
						// topPeaks will contain indexes to mzValues in scan above a
						// threshold defined as : 'scan
						// basePeak Intensity' * percent of base Peak to include
						List<Integer> topPeaksList = new ArrayList<Integer>();
						double highestIntensity = scan.getHighestDataPoint().getIntensity() * basePeakPercent;

						for (int i = 0; i < scanDataPoints.length; i++) {
								// Cancel?
								if (status == TaskStatus.CANCELED) {
										return;
								}

								if ((scanDataPoints[i].getIntensity()) > highestIntensity) {
										// add the peaks
										topPeaksList.add(i);
								}
						}

						// Transfer topPeakList over to array
						Integer[] topPeaks = topPeaksList.toArray(new Integer[topPeaksList.size()]);

						// Default set to pass scan and not add to list
						boolean pass = false;

						/**
						 * Depending on filter conditions these if statements will filter based off of product m/z or
						 * neutral loss or both within a scan. Pass becomes set to true if filter conditions are met
						 * and scan is added to output file and visual plot
						 */

						// Filter based off both m/z and neutral loss if both are not equal
						// to 0
						if (targetedMZ_List.size() > 0 && targetedNF_List.size() > 0
								&& targetedMZ_List.get(0) != 0 && targetedNF_List.get(0) != 0) {
								boolean passA = false;
								boolean passB = false;
								boolean[] booleanValuesA = new boolean[targetedMZ_List.size()];
								boolean[] booleanValuesB = new boolean[targetedNF_List.size()];

								// scan through each m/z within scan m/z peaks
								for (Integer topPeak : topPeaks) {
										// Cancel?
										if (status == TaskStatus.CANCELED) {
												return;
										}

										int peakIndex = topPeak;
										if (peakIndex < 0) {
												break;
										}
										double neutralLoss = scan.getPrecursorMZ() - scanDataPoints[peakIndex].getMZ();

										// scan for all m/z values if more than one, set pass to
										// true if all m/z values are found
										for (int j = 0; j < targetedMZ_List.size(); j++) {
												// Cancel?
												if (status == TaskStatus.CANCELED) {
														return;
												}

												if (mzDifference.getToleranceRange(targetedMZ_List.get(j))
														.contains(scanDataPoints[peakIndex].getMZ())) {
														booleanValuesA[j] = true;
												}
										}

										if (isAllTrue(booleanValuesA)) {
												passA = true;
										}

										// scan for all neutral loss values if more than one, set
										// pass to true if all netural loss
										// values are found
										for (int j = 0; j < targetedNF_List.size(); j++) {
												// Cancel?
												if (status == TaskStatus.CANCELED) {
														return;
												}

												if (mzDifference.getToleranceRange(targetedNF_List.get(j))
														.contains(neutralLoss)) {
														booleanValuesB[j] = true;
												}
										}
										if (isAllTrue(booleanValuesB)) {
												passB = true;
										}

								}
								// if both m/z and neutral loss pass, then total pass becomes
								// set to true, and scan is added
								if (passA && passB) {
										pass = true;
								}


						}
						// if only m/z requirements set, search for m/z and set to pass
						// if found in scan
						else if (targetedMZ_List.size() > 0 && targetedMZ_List.get(0) != 0) {
								boolean[] booleanValues = new boolean[targetedMZ_List.size()];
								for (int peakIndex : topPeaks) {
										if (peakIndex < 0) {
												break;
										}
										for (int j = 0; j < targetedMZ_List.size(); j++) {
												// Cancel?
												if (status == TaskStatus.CANCELED) {
														return;
												}

												if (mzDifference.getToleranceRange(targetedMZ_List.get(j))
														.contains(scanDataPoints[peakIndex].getMZ())) {
														booleanValues[j] = true;
												}
										}
										if (isAllTrue(booleanValues)) {
												pass = true;
										}
								}


						}
						// scan for n/f if both are not searched for and m/z is not
						// searched for
						else if (targetedNF_List.size() > 0 && targetedNF_List.get(0) != 0) {
								boolean[] booleanValues = new boolean[targetedMZ_List.size()];
								for (Integer topPeak : topPeaks) {
										// Cancel?
										if (status == TaskStatus.CANCELED) {
												return;
										}

										int peakIndex = topPeak;
										if (peakIndex < 0) {
												break;
										}
										double neutralLoss = scan.getPrecursorMZ() - scanDataPoints[peakIndex].getMZ();
										for (int j = 0; j < targetedNF_List.size(); j++) {
												// Cancel?
												if (status == TaskStatus.CANCELED) {
														return;
												}

												if (mzDifference.getToleranceRange(targetedNF_List.get(j))
														.contains(neutralLoss)) {
														booleanValues[j] = true;
												}
										}
										if (isAllTrue(booleanValues)) {
												pass = true;
										}

								}


						}
						// If no requirements set, simply ouptut all scans
						else {
								pass = true;
						}
						// If pass is set to true, include scan in output file and visual
						// plot
						if (pass) {
								// Add all data points to visual plot and output file from scan
								for (Integer topPeak : topPeaks) {
										// Cancel?
										if (status == TaskStatus.CANCELED) {
												return;
										}

										int peakIndex = topPeak;

										// if we have a very few peaks, the array may not be full
										if (peakIndex < 0) {
												break;
										}

										CombinedModuleDataPoint newPoint =
												new CombinedModuleDataPoint(scanDataPoints[peakIndex].getMZ(),
														scan.getScanNumber(),
														scan.getPrecursorMZ(), scan.getPrecursorCharge(),
														scan.getRetentionTime());

										dataSeries.get(0).add(newPoint);

										// Grab product ion, precursor ion, and retention time for
										// sending to output file
										String temp = scan.getPrecursorMZ() + ","
												+ scanDataPoints[peakIndex].getMZ() + ","
												+ scan.getRetentionTime();
										// add to output file
										dataListVisual.add(temp);
								}

								// add precursor m/z, retention time, and scan number to output
								// .csv file
								String dataMZ = Double.toString(scan.getPrecursorMZ());
								String dataRT = Double.toString(scan.getRetentionTime());
								String dataNM = Double.toString(scan.getScanNumber());
								String temp = dataMZ + "," + dataRT + "," + dataNM;

								dataList.add(temp);
						}

						processedScans++;
				}
				refresh();
				setStatus(TaskStatus.FINISHED);

		}

		private void refresh() {
				Platform.runLater(this::fireDatasetChanged);
		}

		@Override
		public int getSeriesCount() {
				return dataSeries.size();
		}

		@Override
		public Comparable<Integer> getSeriesKey(int series) {
				return series;
		}

		@Override
		public int getItemCount(int series) {
				return dataSeries.get(series).size();
		}

		@Override
		public Number getX(int series, int item) {
				CombinedModuleDataPoint point = dataSeries.get(series).get(item);
				if (xAxisType.equals(AxisType.PRECURSORIONMZ)) {
						double mz = point.getPrecursorMZ();
						return mz;
				} else {
						return point.getRetentionTime();
				}
		}

		@Override
		public Number getY(int series, int item) {
				CombinedModuleDataPoint point = dataSeries.get(series).get(item);
				return point.getProductMZ();
		}

		public static boolean isAllTrue(boolean[] array) {
				for (boolean b : array) {
						if (!b) {
								return false;
						}
				}
				return true;
		}

		@Override
		public String generateToolTip(XYDataset dataset, int series, int item) {
				return dataSeries.get(series).get(item).getName();
		}

		public void updateOnRangeDataPoints(String rangeType) {
				CombinedModulePlot plot = visualizer.getPlot();
				Range<Double> prRange = plot.getHighlightedPrecursorRange();
				Range<Double> nlRange = plot.getHighlightedNeutralLossRange();

				// Set type of search
				int level = NEUTRALLOSS_LEVEL;
				if (rangeType.equals("HIGHLIGHT_PRECURSOR")) {
						level = PRECURSOR_LEVEL;
				}

				// Clean previous selection
				dataSeries.get(level).clear();

				CombinedModuleDataPoint point;
				boolean b = false;
				for (int i = 0; i < dataSeries.get(RAW_LEVEL).size(); i++) {
						point = dataSeries.get(RAW_LEVEL).get(i);
						// Verify if the point is on range
						if (level == PRECURSOR_LEVEL) {
								b = prRange.contains(point.getPrecursorMass());
						} else {
								b = nlRange.contains(point.getProductMZ());
						}
						if (b) {
								dataSeries.get(level).add(point);
						}
				}

				refresh();
		}
}
