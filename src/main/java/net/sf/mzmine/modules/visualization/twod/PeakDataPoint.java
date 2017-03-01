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

package net.sf.mzmine.modules.visualization.twod;

import net.sf.mzmine.datamodel.DataPoint;

/**
 * This class represents one peak datapoint (retention time and m/z)
 */
class PeakDataPoint {

    private int scanNumber;
    private double rt;
    private DataPoint dataPoint;

    /**
     */
    PeakDataPoint(int scanNumber, double rt, DataPoint dataPoint) {
	this.scanNumber = scanNumber;
	this.rt = rt;
	this.dataPoint = dataPoint;
    }

    int getScanNumber() {
	return scanNumber;
    }

    double getRT() {
	return rt;
    }

    double getMZ() {
	return dataPoint.getMZ();
    }

    double getIntensity() {
	return dataPoint.getIntensity();
    }

}
