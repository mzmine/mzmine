/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

/**
 * Integrated peak area data set
 */
class PeakDataSet extends AbstractXYDataset {

    private Peak peak;
    
    PeakDataSet(Peak peak) {
        this.peak = peak;
    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int arg0) {
        return peak.toString();
    }

    public int getItemCount(int series) {
        return peak.getScanNumbers().length;
    }

    public Number getX(int series, int item) {
        RawDataFile dataFile = peak.getDataFile();
        int scanNumber = peak.getScanNumbers()[item];
        Scan scan = dataFile.getScan(scanNumber);
        return scan.getRetentionTime();
    }

    public Number getY(int series, int item) {
        int scanNumber = peak.getScanNumbers()[item];
        float dataPoint[] = peak.getRawDatapoints(scanNumber);
        return dataPoint[1];
}


}
