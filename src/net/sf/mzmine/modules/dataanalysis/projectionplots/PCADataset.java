package net.sf.mzmine.modules.dataanalysis.projectionplots;

import jmprojection.PCA;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

public class PCADataset extends AbstractXYDataset implements ProjectionPlotDataset {

	double[] component1Coords;
	double[] component2Coords;
	OpenedRawDataFile[] openedRawDataFiles;
	
	
	public PCADataset(PeakList peakList, SimpleParameterSet parameters) {
		int numOfRawData = peakList.getNumberOfRawDataFiles();
		int numOfRows = peakList.getNumberOfRows();
		
		boolean useArea = true;
		if (parameters.getParameterValue(ProjectionPlot.MeasurementType)==ProjectionPlot.MeasurementTypeHeight)
			useArea = false;
		
		// Pickup opened raw data files
		openedRawDataFiles = new OpenedRawDataFile[numOfRawData];
		for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) 
			openedRawDataFiles[fileIndex] = peakList.getRawDataFile(fileIndex);
		
		// Generate matrix of raw data (input to PCA)
		double[][] rawData = new double[numOfRawData][numOfRows];
		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			for (int fileIndex=0; fileIndex<numOfRawData; fileIndex++) {
				OpenedRawDataFile orf = openedRawDataFiles[fileIndex];
				Peak p = peakList.getPeak(rowIndex, orf);
				if (p!=null) {
					if (useArea)
						rawData[fileIndex][rowIndex] = p.getArea();
					else
						rawData[fileIndex][rowIndex] = p.getHeight();
				}
			}
		}
		
		PCA pcaProj = new PCA(rawData);
		
		double[][] result = pcaProj.getState();

		component1Coords = result[0];
		component2Coords = result[1];		
				
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

	public OpenedRawDataFile getOpenedRawDataFile(int item) {
		return openedRawDataFiles[item];
	}
	
}
