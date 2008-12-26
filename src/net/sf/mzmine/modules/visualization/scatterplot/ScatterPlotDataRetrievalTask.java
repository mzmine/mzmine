package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Color;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

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

	public ScatterPlotDataRetrievalTask(String taskDescription,
			ScatterPlotDataSet dataSet, boolean update, int numOfItems) {
		status = TaskStatus.WAITING;
		this.taskDescription = taskDescription;
		this.update = update;
		this.dataSet = dataSet;
		this.numOfItems = numOfItems;
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
			
			Vector<Vector<Integer>> seriesArray = new Vector<Vector<Integer>>();
			Vector<Integer> items;
			Vector<Color> colorVector = new Vector<Color>();
			Integer[][] arraySeriesAndItemsConstruction;
			
			DefaultListModel listModel = dataSet.getSelectionListModel();
			int listLength = listModel.getSize();
			int countSeries = 1;
			colorVector.add(Color.BLUE);

			for (int i = 0; i < listLength; i++) {
				seriesArray.add(new Vector<Integer>());
			}

			String[][] searchValues;
			String[] listNames = dataSet.getListNames();

			for (int i = 0; i < listLength; i++) {
				ListSelectionItem listItem = (ListSelectionItem) listModel.get(i);
				items = seriesArray.get(i);
				
				if (listItem.isAlreadyCompared()){
					if (listItem.hasMatches()){
						Integer[] indexes = listItem.getMatches();
						for (Integer ind: indexes){
							items.add(ind);
						}
						countSeries++;
						colorVector.add(listItem.getColor());
					}
					
					processedItems += listItem.getSearchValues().length * listNames.length;
					continue;
				}
				
				searchValues = listItem.getSearchValues();
				int length1 = searchValues.length;
				String selectionElement, originalElement;
				for (int j = 0; j < length1; j++) {
					selectionElement = searchValues[j][0];
					selectionElement = selectionElement.toUpperCase();

					int length2 = listNames.length;
					for (int k = 0; k < length2; k++) {
						originalElement = listNames[k];
						if ((originalElement == null) || (originalElement == "")) {
							continue;
						}
						originalElement = originalElement.toUpperCase();

						if ((originalElement.matches(".*" + selectionElement + ".*"))) {
							if (!items.contains(k))
								items.add(k);
						}
						
						processedItems++;
					}
				}
				if (items.size() > 0){
					countSeries++;
					colorVector.add(listItem.getColor());
					listItem.setMatches(items.toArray(new Integer[0]));
				}
				listItem.setCompareFlag();
			}


			dataSet.updateSeriesCount(countSeries);
			arraySeriesAndItemsConstruction = new Integer[countSeries][0];
			arraySeriesAndItemsConstruction[0] = dataSet.getArraySeriesAndItems()[0];
			int count = 1;
			for (int i = 0; i < seriesArray.size(); i++) {
				items = seriesArray.get(i);
				if (items.size() > 0) {
					arraySeriesAndItemsConstruction[count] = items.toArray(new Integer[0]);
					count++;
				}
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
