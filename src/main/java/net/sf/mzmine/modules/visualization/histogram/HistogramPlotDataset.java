/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.histogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.statistics.HistogramBin;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

import com.google.common.collect.Range;

public class HistogramPlotDataset extends AbstractIntervalXYDataset {

    private static final long serialVersionUID = 1L;
    private HistogramDataType dataType;
    private PeakList peakList;
    private RawDataFile[] rawDataFiles;
    private int numOfBins;
    private double maximum, minimum;

    /** A list of maps. */
    private Vector<HashMap<?, ?>> list;

    /** The histogram type. */
    private HistogramType type;

    public HistogramPlotDataset(PeakList peakList, RawDataFile[] rawDataFiles,
	    int numOfBins, HistogramDataType dataType, Range<Double> range) {

	this.list = new Vector<HashMap<?, ?>>();
	this.type = HistogramType.FREQUENCY;
	this.dataType = dataType;
	this.peakList = peakList;
	this.numOfBins = numOfBins;
	this.rawDataFiles = rawDataFiles;

	minimum = range.lowerEndpoint();
	maximum = range.upperEndpoint();

	updateHistogramDataset();

    }

    public void updateHistogramDataset() {
	this.list.clear();
	Feature[] peaks;
	double[] values = null;
	for (RawDataFile dataFile : rawDataFiles) {
	    peaks = peakList.getPeaks(dataFile);
	    values = new double[peaks.length];
	    for (int i = 0; i < peaks.length; i++) {
		switch (dataType) {
		case AREA:
		    values[i] = peaks[i].getArea();
		    break;
		case HEIGHT:
		    values[i] = peaks[i].getHeight();
		    break;
		case MASS:
		    values[i] = peaks[i].getMZ();
		    break;
		case RT:
		    values[i] = peaks[i].getRT();
		    break;
		}

	    }
	    addSeries(dataFile.getName(), values);
	}

    }

    public void setNumberOfBins(int numOfBins) {
	this.numOfBins = numOfBins;
    }

    public int getNumberOfBins() {
	return this.numOfBins;
    }

    public void setBinWidth(double binWidth) {
	int numBins;
	updateHistogramDataset();
	double[] values = getValues(0);
	double minimum = getMinimum(values);
	double maximum = getMaximum(values);
	numBins = (int) ((maximum - minimum) / binWidth);
	setNumberOfBins(numBins);
    }

    public double getBinWidth() {
	return this.getBinWidth(0);
    }

    public void setHistogramDataType(HistogramDataType dataType) {
	this.dataType = dataType;
    }

    public PeakList getPeakList() {
	return this.peakList;
    }

    /**
     * Returns the histogram type.
     * 
     * @return The type (never <code>null</code>).
     */
    public HistogramType getType() {
	return this.type;
    }

    /**
     * Sets the histogram type and sends a {@link DatasetChangeEvent} to all
     * registered listeners.
     * 
     * @param type
     *            the type (<code>null</code> not permitted).
     */
    public void setType(HistogramType type) {
	if (type == null) {
	    throw new IllegalArgumentException("Null 'type' argument");
	}
	this.type = type;
	notifyListeners(new DatasetChangeEvent(this, this));
    }

    /**
     * Adds a series to the dataset. Any data value less than minimum will be
     * assigned to the first bin, and any data value greater than maximum will
     * be assigned to the last bin. Values falling on the boundary of adjacent
     * bins will be assigned to the higher indexed bin.
     * 
     * @param key
     *            the series key (<code>null</code> not permitted).
     * @param values
     *            the raw observations.
     * @param numOfBins
     *            the number of bins (must be at least 1).
     * @param minimum
     *            the lower bound of the bin range.
     * @param maximum
     *            the upper bound of the bin range.
     */
    public void addSeries(Comparable<?> key, double[] values) {

	if (key == null) {
	    throw new IllegalArgumentException("Null 'key' argument.");
	}
	if (values == null) {
	    throw new IllegalArgumentException("Null 'values' argument.");
	} else if (numOfBins < 1) {
	    throw new IllegalArgumentException(
		    "The 'bins' value must be at least 1.");
	}
	double binWidth = (maximum - minimum) / numOfBins;

	double lower = minimum;
	double upper;
	List<HistogramBin> binList = new ArrayList<HistogramBin>(numOfBins);
	for (int i = 0; i < numOfBins; i++) {
	    HistogramBin bin;
	    // make sure bins[bins.length]'s upper boundary ends at maximum
	    // to avoid the rounding issue. the bins[0] lower boundary is
	    // guaranteed start from min
	    if (i == numOfBins - 1) {
		bin = new HistogramBin(lower, maximum);
	    } else {
		upper = minimum + (i + 1) * binWidth;
		bin = new HistogramBin(lower, upper);
		lower = upper;
	    }
	    binList.add(bin);
	}
	// fill the bins
	for (int i = 0; i < values.length; i++) {
	    int binIndex = numOfBins - 1;
	    if (values[i] < maximum) {
		double fraction = (values[i] - minimum) / (maximum - minimum);
		if (fraction < 0.0) {
		    fraction = 0.0;
		}
		binIndex = (int) (fraction * numOfBins);
		if (binIndex >= numOfBins) {
		    binIndex = numOfBins - 1;
		}
	    }
	    HistogramBin bin = (HistogramBin) binList.get(binIndex);
	    bin.incrementCount();

	}
	// generic map for each series
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("key", key);
	map.put("bins", binList);
	map.put("values.length", new Integer(values.length));
	map.put("bin width", new Double(binWidth));
	map.put("values", values);
	this.list.add(map);
    }

    /**
     * Returns the minimum value in an array of values.
     * 
     * @param values
     *            the values (<code>null</code> not permitted and zero-length
     *            array not permitted).
     * 
     * @return The minimum value.
     */
    private double getMinimum(double[] values) {
	if (values == null || values.length < 1) {
	    throw new IllegalArgumentException(
		    "Null or zero length 'values' argument.");
	}
	double min = Double.MAX_VALUE;
	for (int i = 0; i < values.length; i++) {
	    if (values[i] < min) {
		min = values[i];
	    }
	}
	return min;
    }

    /**
     * Returns the maximum value in an array of values.
     * 
     * @param values
     *            the values (<code>null</code> not permitted and zero-length
     *            array not permitted).
     * 
     * @return The maximum value.
     */
    private double getMaximum(double[] values) {
	if (values == null || values.length < 1) {
	    throw new IllegalArgumentException(
		    "Null or zero length 'values' argument.");
	}
	double max = -Double.MAX_VALUE;
	for (int i = 0; i < values.length; i++) {
	    if (values[i] > max) {
		max = values[i];
	    }
	}
	return max;
    }

    /**
     * Returns the bins for a series.
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * 
     * @return A list of bins.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    private List<?> getBins(int series) {
	HashMap<?, ?> map = (HashMap<?, ?>) this.list.get(series);
	return (List<?>) map.get("bins");
    }

    /**
     * @param series
     * @return
     */
    private double[] getValues(int series) {
	HashMap<?, ?> map = (HashMap<?, ?>) this.list.get(series);
	return (double[]) map.get("values");
    }

    /**
     * Returns the total number of observations for a series.
     * 
     * @param series
     *            the series index.
     * 
     * @return The total.
     */
    private int getTotal(int series) {
	Map<?, ?> map = (Map<?, ?>) this.list.get(series);
	return ((Integer) map.get("values.length")).intValue();
    }

    /**
     * Returns the bin width for a series.
     * 
     * @param series
     *            the series index (zero based).
     * 
     * @return The bin width.
     */
    private double getBinWidth(int series) {
	Map<?, ?> map = (Map<?, ?>) this.list.get(series);
	return ((Double) map.get("bin width")).doubleValue();
    }

    /**
     * Returns the number of series in the dataset.
     * 
     * @return The series count.
     */
    public int getSeriesCount() {
	return this.list.size();
    }

    /**
     * Returns the key for a series.
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * 
     * @return The series key.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Comparable<?> getSeriesKey(int series) {
	Map<?, ?> map = (Map<?, ?>) this.list.get(series);
	return (Comparable<?>) map.get("key");
    }

    /**
     * Returns the number of data items for a series.
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * 
     * @return The item count.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */

    public int getItemCount(int series) {
	return getBins(series).size();
    }

    /**
     * Returns the X value for a bin. This value won't be used for plotting
     * histograms, since the renderer will ignore it. But other renderers can
     * use it (for example, you could use the dataset to create a line chart).
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The start value.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getX(int series, int item) {
	List<?> bins = getBins(series);
	HistogramBin bin = (HistogramBin) bins.get(item);
	double x = (bin.getStartBoundary() + bin.getEndBoundary()) / 2.;
	return new Double(x);
    }

    /**
     * Returns the y-value for a bin (calculated to take into account the
     * histogram type).
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The y-value.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getY(int series, int item) {
	List<?> bins = getBins(series);
	HistogramBin bin = (HistogramBin) bins.get(item);
	double total = getTotal(series);
	double binWidth = getBinWidth(series);

	if (this.type == HistogramType.FREQUENCY) {
	    return new Double(bin.getCount());
	} else if (this.type == HistogramType.RELATIVE_FREQUENCY) {
	    return new Double(bin.getCount() / total);
	} else if (this.type == HistogramType.SCALE_AREA_TO_1) {
	    return new Double(bin.getCount() / (binWidth * total));
	} else { // pretty sure this shouldn't ever happen
	    throw new IllegalStateException();
	}
    }

    /**
     * Returns the start value for a bin.
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The start value.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getStartX(int series, int item) {
	List<?> bins = getBins(series);
	HistogramBin bin = (HistogramBin) bins.get(item);
	return new Double(bin.getStartBoundary());
    }

    /**
     * Returns the end value for a bin.
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The end value.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getEndX(int series, int item) {
	List<?> bins = getBins(series);
	HistogramBin bin = (HistogramBin) bins.get(item);
	return new Double(bin.getEndBoundary());
    }

    /**
     * Returns the start y-value for a bin (which is the same as the y-value,
     * this method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The y-value.
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getStartY(int series, int item) {
	return getY(series, item);
    }

    /**
     * Returns the end y-value for a bin (which is the same as the y-value, this
     * method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     * 
     * @param series
     *            the series index (in the range <code>0</code> to
     *            <code>getSeriesCount() - 1</code>).
     * @param item
     *            the item index (zero based).
     * 
     * @return The Y value.
     * 
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>series</code> is outside the specified range.
     */
    public Number getEndY(int series, int item) {
	return getY(series, item);
    }

    public double getMinimum() {
	return minimum;
    }

    public double getMaximum() {
	return maximum;
    }

}
