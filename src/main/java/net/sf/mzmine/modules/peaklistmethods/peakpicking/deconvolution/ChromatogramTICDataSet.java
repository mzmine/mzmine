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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

public class ChromatogramTICDataSet extends AbstractXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Feature chromatogram;
    private RawDataFile dataFile;
    private int scanNumbers[];

    public ChromatogramTICDataSet(Feature chromatogram) {
	this.chromatogram = chromatogram;
	this.dataFile = chromatogram.getDataFile();
	this.scanNumbers = chromatogram.getScanNumbers();
    }

    public Comparable<?> getSeriesKey(int series) {
	return chromatogram.toString();
    }

    public int getItemCount(int series) {
	return scanNumbers.length;
    }

    public Number getX(int series, int index) {
	return dataFile.getScan(scanNumbers[index]).getRetentionTime();
    }

    public Number getY(int series, int index) {
	DataPoint mzPeak = chromatogram.getDataPoint(scanNumbers[index]);
	if (mzPeak == null)
	    return 0;
	return mzPeak.getIntensity();
    }

    public int getSeriesCount() {
	return 1;
    }

}
