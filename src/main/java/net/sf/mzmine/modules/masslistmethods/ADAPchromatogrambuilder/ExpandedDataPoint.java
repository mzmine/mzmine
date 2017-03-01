/*
 * Created by Owen Myers (Oweenm@gmail.com)
 */


package net.sf.mzmine.modules.masslistmethods.ADAPchromatogrambuilder;
import net.sf.mzmine.datamodel.DataPoint;


/**
 * DataPoint implementation extended with scan number
 */
public class ExpandedDataPoint implements DataPoint {

    private int scanNumber = -1;
    private double mz, intensity;

    /**
     */
    public ExpandedDataPoint(double mz,  double intensity, int scanNumber) {

	this.scanNumber = scanNumber;
	this.mz = mz;
	this.intensity = intensity;

    }

    /**
     * Constructor which copies the data from another DataPoint
     */
    public ExpandedDataPoint(DataPoint dp) {
	this.mz = dp.getMZ();
	this.intensity = dp.getIntensity();
    }

    /**
     * Constructor which copies the data from another DataPoint and takes the scan number
     */
    public ExpandedDataPoint(DataPoint dp, int scanNumIn) {
	this.mz = dp.getMZ();
	this.intensity = dp.getIntensity();
    this.scanNumber = scanNumIn;
    }

    public ExpandedDataPoint(){
    this.mz = 0.0;
	this.intensity = 0.0;
    this.scanNumber = -1;
    }

    @Override
    public double getIntensity() {
	return intensity;
    }

    @Override
    public double getMZ() {
	return mz;
    }
    public int getScanNumber() {
	return scanNumber;
    }



}
