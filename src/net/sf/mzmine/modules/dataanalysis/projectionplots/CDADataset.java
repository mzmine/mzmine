package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import jmprojection.CDA;
import jmprojection.PCA;
import jmprojection.Preprocess;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

public class CDADataset extends AbstractXYDataset implements
		ProjectionPlotDataset {

	private double[] component1Coords;
	private double[] component2Coords;
	private Color[] colors;
	private RawDataFile[] selectedRawDataFiles;

	private String datasetTitle;
	private int xAxisDimension;
	private int yAxisDimension;


	public CDADataset(ProjectionPlotParameters parameters, int xAxisDimension, int yAxisDimension) {

		this.xAxisDimension = xAxisDimension;
		this.yAxisDimension = yAxisDimension;

		int numOfRawData = parameters.getSelectedDataFiles().length;
		int numOfRows = parameters.getSelectedRows().length;

		boolean useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakAreaOption)
			useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakHeightOption)
			useArea = false;
		
		datasetTitle = "Curvilinear distance analysis";

		selectedRawDataFiles = parameters.getSelectedDataFiles();
		
		// Set colors for files
		Hashtable<RawDataFile, Color> colorsForFiles = parameters.getColorsForSelectedFiles();
		colors = new Color[parameters.getSelectedDataFiles().length];
		for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
			Color color = colorsForFiles.get(selectedRawDataFiles[fileIndex]);
			colors[fileIndex] = color;
		}

		PeakListRow[] selectedPeakListRows = parameters.getSelectedRows();
		
		// Generate matrix of raw data (input to CDA)
		double[][] rawData = new double[numOfRawData][numOfRows];
		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			PeakListRow peakListRow = selectedPeakListRows[rowIndex];
			for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
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

		//// DEBUG		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("C:\\temp\\inputtocda.txt")));
		} catch (IOException ex) {
			System.err.println("" + ex.toString());
		}

		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
				writer.print(rawData[fileIndex][rowIndex]);
				if (fileIndex<(numOfRawData-1))
					writer.print("\t");
			}
			writer.println();
		}
		writer.flush();
		writer.close();
		///// DEBUG
		
		
		// Scale data and do CDA
		Preprocess.scaleToUnityVariance(rawData);
		CDA cdaProj = new CDA(rawData);

		double[][] result = cdaProj.getState();

		component1Coords = result[xAxisDimension-1];
		component2Coords = result[yAxisDimension-1];
	
		///// DEBUG
		/*
		writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("C:\\temp\\outputfromcda.txt")));
		} catch (IOException ex) {
			System.err.println("" + ex.toString());
		}

		for (int componentIndex=0; componentIndex<2; componentIndex++) {
			for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
				writer.print(result[componentIndex][fileIndex]);
				if (fileIndex<(numOfRows-1))
					writer.print("\t");
			}
			writer.println();
		}
		writer.flush();
		writer.close();
		*/
		//// DEBUG

	}

	public String toString() {
		return datasetTitle;
	}

	public Color getColor(int item) {
		return colors[item];
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

}
