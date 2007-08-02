package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.util.HashMap;
import java.util.Hashtable;

import jmprojection.PCA;
import jmprojection.Preprocess;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

public class PCADataset extends AbstractXYDataset implements ProjectionPlotDataset {

	private double[] component1Coords;
	private double[] component2Coords;
	private Color[] colors;
	private RawDataFile[] selectedRawDataFiles;

	private String datasetTitle;
	private int xAxisPC;
	private int yAxisPC;


	public PCADataset(ProjectionPlotParameters parameters, int xAxisPC, int yAxisPC) {

		this.xAxisPC = xAxisPC;
		this.yAxisPC = yAxisPC;

		int numOfRawData = parameters.getSelectedDataFiles().length;
		int numOfRows = parameters.getSelectedRows().length;

		boolean useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakAreaOption)
			useArea = true;
		if (parameters.getPeakMeasuringMode()==parameters.PeakHeightOption)
			useArea = false;
		
		datasetTitle = "Principal component analysis";

		
		selectedRawDataFiles = parameters.getSelectedDataFiles();
		
		// Set colors for files
		Hashtable<RawDataFile, Color> colorsForFiles = parameters.getColorsForSelectedFiles();
		colors = new Color[parameters.getSelectedDataFiles().length];
		for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
			Color color = colorsForFiles.get(selectedRawDataFiles[fileIndex]);
			colors[fileIndex] = color;
		}

		PeakListRow[] selectedPeakListRows = parameters.getSelectedRows();
		
		// Generate matrix of raw data (input to PCA)
		double[][] rawData = new double[numOfRawData][numOfRows];
		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
				RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
				PeakListRow peakListRow = selectedPeakListRows[fileIndex];
				Peak p = peakListRow.getPeak(rawDataFile);
				if (p!=null) {
					if (useArea)
						rawData[fileIndex][rowIndex] = p.getArea();
					else
						rawData[fileIndex][rowIndex] = p.getHeight();
				}
			}
		}

		int numComponents = xAxisPC;
		if (yAxisPC>numComponents) numComponents = yAxisPC;

		// Scale data and do PCA
		Preprocess.scaleToUnityVariance(rawData);
		PCA pcaProj = new PCA(rawData, numComponents);

		double[][] result = pcaProj.getState();

		component1Coords = result[xAxisPC-1];
		component2Coords = result[yAxisPC-1];

		//// DEBUG
		/*
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("C:\\temp\\inputtopca.txt")));
		} catch (IOException ex) {
			System.err.println("" + ex.toString());
		}

		for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
			for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
				writer.print(rawData[fileIndex][rowIndex]);
				if (rowIndex<(numOfRows-1))
					writer.print("\t");
			}
			writer.println();
		}
		writer.flush();
		writer.close();

		writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("C:\\temp\\outputfrompca.txt")));
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
		if (xAxisPC==1) return "1st PC";
		if (xAxisPC==2) return "2nd PC";
		if (xAxisPC==3) return "3rd PC";
		return "" + xAxisPC + "th PC";
	}

	public String getYLabel() {
		if (yAxisPC==1) return "1st PC";
		if (yAxisPC==2) return "2nd PC";
		if (yAxisPC==3) return "3rd PC";
		return "" + yAxisPC + "th PC";
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
