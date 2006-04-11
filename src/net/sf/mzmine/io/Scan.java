/*
 * Copyright 2005 VTT Biotechnology
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.io;



/**
 * This class represent one spectrum of a raw data file.
 * The implementing class is supposed to be immutable.
 * 
 */
public interface Scan {

    public RawDataFile getRawData();

    public int getScanNumber();

    public int getMSLevel();

    public double getPrecursorMZ();

    public double getPrecursorRT();

    public int getPrecursorScanNumber();

    public double getScanAcquisitionTime();

    public double getScanDuration();

    public double getMZRangeMin();

    public double getMZRangeMax();

    public int getNumberOfDataPoints();

    public double[] getMZValues();

    public double[] getIntensityValues();

    public double getBasePeakMZ();

    public double getBasePeakIntensity();

    public double getTotalIonCurrent();

}
