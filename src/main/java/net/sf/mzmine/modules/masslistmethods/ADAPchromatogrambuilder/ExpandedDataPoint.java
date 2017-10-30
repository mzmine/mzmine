/*
 * Copyright (c) 2017 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */



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
