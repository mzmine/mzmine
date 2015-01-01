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

package net.sf.mzmine.modules.visualization.spectra.datasets;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Data set for isotope pattern
 */
public class IsotopesDataSet extends AbstractXYDataset implements
	IntervalXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private IsotopePattern isotopePattern;
    private DataPoint[] dataPoints;
    private String label;

    public IsotopesDataSet(IsotopePattern isotopePattern) {

	dataPoints = isotopePattern.getDataPoints();

	label = "Isotopes (" + isotopePattern.getNumberOfDataPoints() + ") "
		+ isotopePattern.getDescription();

	this.isotopePattern = isotopePattern;

    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    @Override
    public int getSeriesCount() {
	return 1;
    }

    @Override
    public Comparable<?> getSeriesKey(int series) {
	return label;
    }

    public int getItemCount(int series) {
	return dataPoints.length;
    }

    public Number getX(int series, int item) {
	return dataPoints[item].getMZ();
    }

    public Number getY(int series, int item) {
	return dataPoints[item].getIntensity();
    }

    public Number getEndX(int series, int item) {
	return getX(series, item).doubleValue();
    }

    public double getEndXValue(int series, int item) {
	return getX(series, item).doubleValue();
    }

    public Number getEndY(int series, int item) {
	return getY(series, item);
    }

    public double getEndYValue(int series, int item) {
	return getYValue(series, item);
    }

    public Number getStartX(int series, int item) {
	return getX(series, item).doubleValue();
    }

    public double getStartXValue(int series, int item) {
	return getX(series, item).doubleValue();
    }

    public Number getStartY(int series, int item) {
	return getY(series, item);
    }

    public double getStartYValue(int series, int item) {
	return getYValue(series, item);
    }

}
