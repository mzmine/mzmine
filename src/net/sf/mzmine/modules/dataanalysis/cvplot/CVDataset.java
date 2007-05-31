package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

import org.jfree.data.xy.AbstractXYZDataset;

public class CVDataset extends AbstractXYZDataset {

	private double[] xCoords = new double[0];
	private double[] yCoords = new double[0];
	private double[] colorCoords = new double[0];
	
	public CVDataset(PeakList alignedPeakList, OpenedRawDataFile[] selectedFiles) {
		int numOfRows = alignedPeakList.getNumberOfRows();

		Vector<Double> xCoordsV = new Vector<Double>();
		Vector<Double> yCoordsV = new Vector<Double>();
		Vector<Double> colorCoordsV = new Vector<Double>();
		
		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			
			PeakListRow row = alignedPeakList.getRow(rowIndex);
			
			// Collect available peak intensities for selected files
			Vector<Double> peakIntensities = new Vector<Double>(); 
			for (int fileIndex=0; fileIndex<selectedFiles.length; fileIndex++) {
				Peak p = row.getPeak(selectedFiles[fileIndex]);
				if (p!=null) {
					// TODO: Perhaps there should be a method specific parameter for switching between computation using heights / areas.
					peakIntensities.add(p.getArea());
				}
			}
			
			// If there are at least two measurements available for this peak then calc CV and include this peak in the plot
			if (peakIntensities.size()>1) {
				double[] ints = CollectionUtils.toDoubleArray(peakIntensities);
				Double cv = MathUtils.calcCV(ints);
				
				Double rt = row.getAverageRT();
				Double mz = row.getAverageMZ();
				
				xCoordsV.add(rt);
				yCoordsV.add(mz);
				colorCoordsV.add(cv);
				
			} 
	
		}

		// Finally store all collected values in arrays
		xCoords = CollectionUtils.toDoubleArray(xCoordsV);
		yCoords = CollectionUtils.toDoubleArray(yCoordsV);
		colorCoords = CollectionUtils.toDoubleArray(colorCoordsV);
		
	}
	
	
	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		if (series==0) return new Integer(1); else return null;
	}

	public Number getZ(int series, int item) {
		if (series!=0) return null;
		if ((colorCoords.length-1)<item) return null;
		return colorCoords[item];
	}

	public int getItemCount(int series) {
		return xCoords.length;
	}

	public Number getX(int series, int item) {
		if (series!=0) return null;
		if ((xCoords.length-1)<item) return null;
		return xCoords[item];
	}

	public Number getY(int series, int item) {
		if (series!=0) return null;
		if ((yCoords.length-1)<item) return null;
		return yCoords[item];	
	}

}
