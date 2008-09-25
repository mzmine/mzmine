package net.sf.mzmine.data;

import net.sf.mzmine.util.Range;

public interface MzDataTable {
	
    /**
     * Returns data points of this scan sorted in m/z order.
     * 
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this scan
     */
    public DataPoint[] getDataPoints();

    /**
     * Returns data points in given m/z range, sorted in m/z order.
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this MzDataTable
     */
    //public DataPoint[] getDataPointsByMass(Range mzRange);

    /**
     * Returns data points over given intensity, sorted in m/z order.
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this MzDataTable
     */
    //public DataPoint[] getDataPointsOverIntensity(float intensity);

    /**
     * 
     * @return Number of m/z and intensity data points
     */
    public int getNumberOfDataPoints();


}
