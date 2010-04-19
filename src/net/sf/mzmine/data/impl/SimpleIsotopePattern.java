/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.util.ScanUtils;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

	private int charge;
	private DataPoint dataPoints[], highestIsotope;
	private IsotopePatternStatus status;
	private String description;

	public SimpleIsotopePattern(int charge, DataPoint dataPoints[],
			IsotopePatternStatus status, String description) {

		assert dataPoints.length > 0;

		highestIsotope = ScanUtils.findTopDataPoint(dataPoints);
		this.charge = charge;
		this.dataPoints = dataPoints;
		this.status = status;
		this.description = description;
	}

	public int getCharge() {
		return charge;
	}

	public DataPoint[] getDataPoints() {
		return dataPoints;
	}

	public int getNumberOfIsotopes() {
		return dataPoints.length;
	}

	public IsotopePatternStatus getStatus() {
		return status;
	}

	public IsotopePattern normalizeTo(double normalizedValue) {

		double maxIntensity = highestIsotope.getIntensity();

		DataPoint newDataPoints[] = new DataPoint[dataPoints.length];

		for (int i = 0; i < dataPoints.length; i++) {

			double mz = dataPoints[i].getMZ();
			double intensity = dataPoints[i].getIntensity() / maxIntensity
					* normalizedValue;

			newDataPoints[i] = new SimpleDataPoint(mz, intensity);
		}

		SimpleIsotopePattern newPattern = new SimpleIsotopePattern(charge,
				newDataPoints, status, description);

		return newPattern;
	}

	public DataPoint getHighestIsotope() {
		return highestIsotope;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return "Isotope pattern: " + description;
	}

}