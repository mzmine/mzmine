package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import jmprojection.CDA;
import jmprojection.PCA;
import jmprojection.Preprocess;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;

import org.jfree.data.xy.AbstractXYDataset;

public class CDADataset extends AbstractXYDataset implements
		ProjectionPlotDataset {

	private double[] component1Coords;
	private double[] component2Coords;

	private Parameter selectedParameter;
	
	private RawDataFile[] selectedRawDataFiles;
	private PeakListRow[] selectedRows;
	
	private int[] groupsForSelectedRawDataFiles;
	private Object[] parameterValuesForGroups;
	int numberOfGroups;
	
	private String datasetTitle;
	private int xAxisDimension;
	private int yAxisDimension;


	public CDADataset(ProjectionPlotParameters parameters, int xAxisDimension, int yAxisDimension) {

		this.xAxisDimension = xAxisDimension;
		this.yAxisDimension = yAxisDimension;

		selectedParameter = parameters.getSelectedParameter();
		selectedRawDataFiles = parameters.getSelectedDataFiles();
		selectedRows = parameters.getSelectedRows();

		boolean useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakAreaOption)
			useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakHeightOption)
			useArea = false;
		
		datasetTitle = "Curvilinear distance analysis";

		
		// Determine groups for selected raw data files
		groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];
		
		if (parameters.getColoringMode()==parameters.ColoringSingleOption) {
			// All files to a single group
			for (int ind=0; ind<selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = 0;
			
			numberOfGroups = 1;	
		}

		if (parameters.getColoringMode()==parameters.ColoringByFileOption) {
			// Each file to own group
			for (int ind=0; ind<selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = ind;
			
			numberOfGroups = selectedRawDataFiles.length;
		}		
		
		if (parameters.getColoringMode()==parameters.ColoringByParameterValueOption) {
			// Group files with same parameter value to same group
			MZmineProject project = MZmineCore.getCurrentProject();
			Vector<Object> availableParameterValues = new Vector<Object>(); 
			for (RawDataFile rawDataFile : selectedRawDataFiles) {
				Object paramValue = project.getParameterValue(selectedParameter, rawDataFile);
				if (!availableParameterValues.contains(paramValue))
					availableParameterValues.add(paramValue);
			}
			
			for (int ind=0; ind<selectedRawDataFiles.length; ind++) {
				Object paramValue = project.getParameterValue(selectedParameter, selectedRawDataFiles[ind]);
				groupsForSelectedRawDataFiles[ind] = availableParameterValues.indexOf(paramValue);  
			}
			parameterValuesForGroups = availableParameterValues.toArray();
			
			numberOfGroups = parameterValuesForGroups.length;
		}

		
		// Generate matrix of raw data (input to PCA)
		double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
		for (int rowIndex=0; rowIndex<selectedRows.length; rowIndex++) {
			PeakListRow peakListRow = selectedRows[rowIndex];
			for (int fileIndex=0; fileIndex<selectedRawDataFiles.length; fileIndex++) {
				RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
				Peak p = peakListRow.getPeak(rawDataFile);
				if (p!=null) {
					if (useArea)
						rawData[fileIndex][rowIndex] = p.getArea();
					else
						rawData[fileIndex][rowIndex] = p.getHeight();
				}
			}
		}

		int numComponents = xAxisDimension;
		if (yAxisDimension>numComponents) numComponents = yAxisDimension;

		// Scale data and do CDA
		Preprocess.scaleToUnityVariance(rawData);
		CDA cdaProj = new CDA(rawData);
		cdaProj.iterate(100);

		double[][] result = cdaProj.getState();


		component1Coords = result[xAxisDimension-1];
		component2Coords = result[yAxisDimension-1];

	}

	public String toString() {
		return datasetTitle;
	}


	public String getXLabel() {
		if (xAxisDimension==1) return "1st projected dimension";
		if (xAxisDimension==2) return "2nd projected dimension";
		if (xAxisDimension==3) return "3rd projected dimension";
		return "" + xAxisDimension + "th projected dimension";
	}

	public String getYLabel() {
		if (yAxisDimension==1) return "1st projected dimension";
		if (yAxisDimension==2) return "2nd projected dimension";
		if (yAxisDimension==3) return "3rd projected dimension";
		return "" + yAxisDimension + "th projected dimension";
	}


	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return 1;
	}

	public int getItemCount(int series) {
		return component1Coords.length;
	}

	public Number getX(int series, int item) {
		return component1Coords[item];
	}

	public Number getY(int series, int item) {
		return component2Coords[item];
	}

	public RawDataFile getRawDataFile(int item) {
		return selectedRawDataFiles[item];
	}

	public int getGroupNumber(int item) {
		return groupsForSelectedRawDataFiles[item];
	}

	public Object getGroupParameterValue(int groupNumber) {
		if (parameterValuesForGroups==null) return null;
		if ((parameterValuesForGroups.length-1)<groupNumber) return null;		
		return parameterValuesForGroups[groupNumber];
	}

	public int getNumberOfGroups() {
		return numberOfGroups;
	}	

}
