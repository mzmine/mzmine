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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Color;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.taskcontrol.Task;

public class ScatterPlotDataRetrievalTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private ScatterPlotDataSet dataSet;
	private TaskStatus status;
	private String errorMessage, taskDescription;
	private int processedItems, numOfItems;
	private boolean update = false;
	private String searchValue;

	public ScatterPlotDataRetrievalTask(String taskDescription,
			ScatterPlotDataSet dataSet, boolean update, int numOfItems, String searchValue) {
		status = TaskStatus.WAITING;
		this.taskDescription = taskDescription;
		this.update = update;
		this.dataSet = dataSet;
		this.numOfItems = numOfItems;
		this.searchValue = searchValue;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return taskDescription;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return (double) processedItems / numOfItems;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getResult()
	 */
	public Object getResult() {
		// this task has no result
		return null;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public void run() {
		logger.finest("Starting new raw data retrieval task");

		status = TaskStatus.PROCESSING;
		PeakListRow row;

		if (update) {
			
			Vector<Integer> items = new Vector<Integer>();
			Vector<Color> colorVector = new Vector<Color>();
			Integer[][] arraySeriesAndItemsConstruction;
			
			colorVector.add(Color.BLUE);

			//String searchValue;
			String[] listNames = dataSet.getListNames();

			int length1 = listNames.length;
			String selectionElement, originalElement;
			selectionElement = searchValue;
			selectionElement = selectionElement.toUpperCase();
			for (int j = 0; j < length1; j++) {
				originalElement = listNames[j];
				if ((originalElement == null) || (originalElement == "")) {
					continue;
				}
				originalElement = originalElement.toUpperCase();
				if ((originalElement.matches(".*" + selectionElement + ".*"))) {
					if (!items.contains(j))
						items.add(j);
				}
					
				processedItems++;
			}
				
		
			if (items.size() > 0){
				colorVector.add(Color.ORANGE);
				dataSet.updateSeriesCount(2);
				arraySeriesAndItemsConstruction = new Integer[2][0];
				arraySeriesAndItemsConstruction[0] = dataSet.getArraySeriesAndItems()[0];
				arraySeriesAndItemsConstruction[1] = items.toArray(new Integer[0]);
			}
			else{
				dataSet.updateSeriesCount(1);
				arraySeriesAndItemsConstruction = new Integer[1][0];
				arraySeriesAndItemsConstruction[0] = dataSet.getArraySeriesAndItems()[0];
			}

			dataSet.updateSeriesColor(colorVector.toArray(new Color[0]));
			dataSet.updateArraySeriesAndItems(arraySeriesAndItemsConstruction);			


		} else {
			
			PeakList peakList = dataSet.getPeakList();
			
			for (int i = 0; i < numOfItems; i++) {

				if (status == TaskStatus.CANCELED)
					return;

				try {

					row = peakList.getRow(i);
					dataSet.addRow(row, i);

				} catch (Throwable e) {

					logger
							.log(
									Level.WARNING,
									"Peak list data retrieval task caught an exception",
									e);
					status = TaskStatus.ERROR;
					errorMessage = e.toString();
					return;
				}

				processedItems++;

			}
			dataSet.fixLowestIntensity();
		}

		status = TaskStatus.FINISHED;

		logger.finest("Raw data retrieval task finished");

	}

}
