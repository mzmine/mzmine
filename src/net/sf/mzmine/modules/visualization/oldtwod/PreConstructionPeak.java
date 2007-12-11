package net.sf.mzmine.modules.visualization.oldtwod;

import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.io.RawDataFile;

/**
 * This class extends ContructionPeak by including pre-peak information. The
 * pre-peak information are m/z ratio and start and stop RT of the peak,
 * which are defined by the user when drawing the peak on 2d plot. Once the
 * drawn pre-peaks are finalized, datapoints of raw data file must be added
 * using methods of the ConstructionPeak class.
 * 
 */
public class PreConstructionPeak extends ConstructionPeak {

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

}
