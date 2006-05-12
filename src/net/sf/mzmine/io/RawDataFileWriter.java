/*
 * Copyright 2006 The MZmine Development Team
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

import java.io.File;
import java.io.IOException;

import net.sf.mzmine.interfaces.Scan;

/**
 *
 */
public interface RawDataFileWriter {


    /**
     * Creates a new scan and adds it to the file.
     * @param	scanNumber	Scan number
     * @param	msLevel		MS level of the scan
     * @param	precursorMZ	Precursor M/Z
     * @param	retentionTime	Retention time of the scan
     * @param	basePeakMZ	Base peak's M/Z
     * @param	basePeakIntensity	Base peak's intensity
     * @param	mzValues	mass values of datapoints
     * @param	intensityValues	intensity values of datapoints
     * @param	centroided	True if scan is centroided
     * @return Newly created scan object
     */
    public Scan createScan(	int scanNumber,
    						int msLevel,
    						double precursorMZ,
    						double retentionTime,
    						double basePeakMZ,
    						double basePeakIntensity,
    						double[] mzValues,
    						double[] intensityValues,
    						boolean centroided
    						) throws IOException;

    /**
     * Finishes writing of the file
     * @return newly written file as RawDataFile
     */
    public RawDataFile finishWriting() throws IOException;


}
