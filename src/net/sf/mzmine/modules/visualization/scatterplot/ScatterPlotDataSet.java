/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

import org.jfree.data.xy.AbstractXYDataset;

public class ScatterPlotDataSet extends AbstractXYDataset implements
		TaskListener {
	
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList peakList;
	private int domainX, domainY;
	private DefaultListModel listModel;
	private int series = 1;
	private Integer[][] arraySeriesAndItems;
	private Color[] seriesColor;

	private TreeMap<Integer, ChromatographicPeak> dataPointsMap;
	private RawDataFile[] rawDataFiles;
	private String[] infotags;
	private String[] listNames;
	private Integer[] items;
	private double[][] intensities;
	private double lowestIntensity;

	private ActionListener visualizer;

	public ScatterPlotDataSet(PeakList peakList, int domainX, int domainY,
			JList listSelections, ActionListener visualizer) {

		this.peakList = peakList;
		this.domainX = domainX;
		this.domainY = domainY;
		this.visualizer = visualizer;
		listModel = (DefaultListModel) listSelections.getModel();
		rawDataFiles = peakList.getRawDataFiles();

		int numOfRows = peakList.getNumberOfRows();
		items = new Integer[numOfRows];
		listNames = new String[numOfRows];
		infotags = new String[numOfRows];
		intensities = new double[numOfRows][rawDataFiles.length];

		arraySeriesAndItems = new Integer[series][numOfRows];
		arraySeriesAndItems[0] = items;
		seriesColor = new Color[1];
		seriesColor[0] = Color.BLUE;
		lowestIntensity = Double.MAX_VALUE;

		// Start-up the refresh task
		Task updateTask = new ScatterPlotDataRetrievalTask(
				"Updating scatter plot of " + peakList.toString(), this, false,
				peakList.getNumberOfRows());
		MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
				this);

	}

	@Override
	public int getSeriesCount() {
		return series;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return 1;
	}

	public int getItemCount(int series) {
		return arraySeriesAndItems[series].length;
	}

	public Number getX(int series, int item) {
		int index = arraySeriesAndItems[series][item];
		return intensities[index][domainX];
	}

	public Number getY(int series, int item) {
		int index = arraySeriesAndItems[series][item];
		return intensities[index][domainY];
	}

	public PeakListRow getPeakListRow(int series, int item) {
		int index = arraySeriesAndItems[series][item];
		return peakList.getRow(index);
	}

	public int getArrayIndex(int series, int item) {
		int index = arraySeriesAndItems[series][item];
		return index;
	}

	public PeakList getPeakList() {
		return peakList;
	}

	public void setDomainsIndexes(int domainX, int domainY) {
		this.domainX = domainX;
		this.domainY = domainY;
	}

	public int[] getDomainsIndexes() {
		int[] domains = new int[2];
		domains[0] = domainX;
		domains[1] = domainY;
		return domains;
	}

	public String[] getDomainsNames() {
		Vector<String> domainsNames = new Vector<String>();
		for (RawDataFile file : rawDataFiles) {
			domainsNames.add(file.getFileName());
		}
		return domainsNames.toArray(new String[0]);
	}

	public String getDataPointName(int index) {
		return listNames[index];
	}

	public String getDataPointName(int series, int item) {
		int index = arraySeriesAndItems[series][item];
		return listNames[index];
	}

	public String[] getListNames() {
		return listNames;
	}
	
	public DefaultListModel getSelectionListModel(){
		return listModel;
	}
	
	public void updateSeriesCount(int series){
		this.series = series;
	}
	
	public void updateSeriesColor(Color[] colors){
		this.seriesColor = colors;
	}
	
	public Integer[][] getArraySeriesAndItems(){
		return arraySeriesAndItems;
	}
	
	public void updateArraySeriesAndItems(Integer[][] array){
		this.arraySeriesAndItems = array;
	}
	
	public double[] getRowIntensities(int item){
		return intensities[item];
	}

	/**
	 * Returns index of data point which exactly matches given X and Y values
	 * 
	 * @param domainX
	 * @param domainY
	 * @return
	 */
	public int getIndex(float valueX, float valueY) {
		for (int i = 0; i < intensities.length; i++) {
			double originalValueX = intensities[i][domainX];
			double originalValueY = intensities[i][domainY];

			if ((Math.abs(valueX - originalValueX) < 0.0000001f)
					&& (Math.abs(valueY - originalValueY) < 0.0000001f))
				return i;
		}
		return -1;
	}

	public double[] getMaxMinValue() {
		int length = peakList.getNumberOfRows();
		double valueX, valueY;
		double maxValue = 0, minValue = Double.MAX_VALUE;
		double[] maxMinValue = new double[2];
		for (int i = 0; i < length; i++) {

			valueX = intensities[i][domainX];
			
			valueY = intensities[i][domainY];
			
			if ((valueX > maxValue) || (valueY > maxValue)) {
				if (valueX > valueY)
					maxValue = valueX;
				else
					maxValue = valueY;
			}
			if ((valueX < minValue) || (valueY < minValue)) {
				if (valueX < valueY)
					minValue = valueX;
				else
					minValue = valueY;
			}
		}

		maxMinValue[0] = minValue;
		maxMinValue[1] = maxValue;

		return maxMinValue;
	}

	public Color getRendererColor(int series) {
		return seriesColor[series];
	}

	public void updateListofAppliedSelection() {

		int numOfItems=0;
		int listLength = listModel.getSize();
		ListSelectionItem listItem;
		for (int i = 0; i < listLength; i++) {
			listItem = (ListSelectionItem) listModel.get(i);
			numOfItems += listItem.getSearchValues().length;
		}
		
		numOfItems *= listNames.length;
		
		// Start-up the refresh task
		Task updateTask = new ScatterPlotDataRetrievalTask(
				"Updating scatter plot of " + peakList.toString(), this, true,
				numOfItems);
		MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
				this);

	}

	public void taskFinished(Task task) {
		if (task.getStatus() == TaskStatus.ERROR) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Error while updating scatter plot: "
							+ task.getErrorMessage());
			return;
		}
		visualizer.actionPerformed(new ActionEvent(this,
				ActionEvent.ACTION_PERFORMED, "ScatterPlotDataSet_upgraded"));
	}

	public void taskStarted(Task task) {
		// ignore
	}

	public void addRow(PeakListRow row, int rowNumber) {
		items[rowNumber] = new Integer(rowNumber);
		PeakIdentity identity = row.getPreferredCompoundIdentity();

		for (int i = 0; i < rawDataFiles.length; i++) {
			ChromatographicPeak peak = row.getPeak(rawDataFiles[i]);
			if (peak != null){
				intensities[rowNumber][i] = peak.getHeight();
				if (intensities[rowNumber][i] < lowestIntensity)
					lowestIntensity = intensities[rowNumber][i];
			}
			else
				intensities[rowNumber][i] = -1;
		}
		
		

		if (identity != null) {
			listNames[rowNumber] = identity.getName();
			infotags[rowNumber] = identity.getCompoundFormula()
					+ " identification method: "
					+ identity.getIdentificationMethod();
		} else {
			listNames[rowNumber] = null;
			infotags[rowNumber] = null;
		}
	}
	
	public void fixLowestIntensity(){
		
		logger.finest("Lowest value " + lowestIntensity);
		int lengthA = intensities.length;
		int lengthB = intensities[0].length;
		for (int i=0;i<lengthA; i++){
			for (int j=0;j<lengthB; j++){
				if (intensities[i][j] == -1){
					intensities[i][j] = lowestIntensity/2.0;
				}
			}
		}
	}

}
