package net.sf.mzmine.modules.visualization.oldtwod;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;

/**
 * This class extends ContructionPeak by including pre-peak information. The
 * pre-peak information are m/z ratio and start and stop RT of the peak,
 * which are defined by the user when drawing the peak on 2d plot. Once the
 * drawn pre-peaks are finalized, datapoints of raw data file must be added
 * using methods of the ConstructionPeak class.
 * 
 */
public class PreConstructionPeak extends ConstructionPeak  implements RawDataAcceptor {

	
	// m/z tolerance value
	private float mzTolerance;

	// m/z value of the line drawn on the 2d-plot
	private float preMZ;

	// Start and stop RT of the line drawn on the 2d-plot
	private float preStartRT;
	private float preStopRT;

	public PreConstructionPeak(RawDataFile rawDataFile, float preMZ,
			float preStartRT, float preStopRT) {
		super(rawDataFile);
		this.preMZ = preMZ;
		this.preStartRT = preStartRT;
		this.preStopRT = preStopRT;
		// TODO: Add m/z tolerance parameter to setup dialog, and pass the value to here
		this.mzTolerance = 0.25f;
	}

	public float getPreMZ() {
		return preMZ;
	}

	public float getPreStartRT() {
		return preStartRT;
	}

	public float getPreStopRT() {
		return preStopRT;
	}
	

	/**
	 * Checks if the scan is within the range of this peak
	 * If yes, then adds the nearest data point (if any within tolerances) to this peak
	 */
	public void addScan(Scan scan, int index, int total) {

		
		
		
		// Is this scan within the range of this peak?
		if ( (scan.getRetentionTime()>=preStartRT) &&
			 (scan.getRetentionTime()<=preStopRT) ) {
				
			// TODO: Change this: locate most intense datapoint within tolerances (instead of nearest in m/z)
			// Locate the nearest datapoint to preMZ
			float nearestDatapointMZ = -1.0f;
			float nearestDatapointInt = 0.0f;
			float nearestMZDifference = Float.MAX_VALUE;
			float previousMZDifference = Float.MAX_VALUE;
			
			float[] scanMZs = scan.getMZValues();
			float[] scanInts = scan.getIntensityValues();
			for (int i=0; i<scanMZs.length; i++) {
				float currentMZDifference = java.lang.Math.abs(scanMZs[i] - preMZ);
				if (currentMZDifference>previousMZDifference) 
					break;
				previousMZDifference = currentMZDifference;
				nearestMZDifference = currentMZDifference;
				nearestDatapointMZ = scanMZs[i];
				nearestDatapointInt = scanInts[i];
				
			}
		
			// If nearest datapoint is within m/z tolerance, then add this datapoint to this peak
			if (nearestMZDifference<mzTolerance)
				this.addDatapoint(scan.getScanNumber(), nearestDatapointMZ, scan.getRetentionTime(), nearestDatapointInt);
			
		}
		
		if (index==total)
			this.finalizedAddingDatapoints();
		
	}

}
