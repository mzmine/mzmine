/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.datastructures;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;

// Java packages
import java.io.Serializable;

/**
 * This class represent raw data file as it is stored during transit node->controller->client
 */
public class RawDataOnTransit implements Serializable {

	// Identity
	public int rawDataID;
	public String niceName;

	// Properties of the raw data
	public int numberOfScans;						// Number of MS-scans in the raw data file
	public double[] scanTimes;						// Times of scans (in secs)
	public double minMZValue;						// Minimum and maximum m/z value in the raw data file
	public double maxMZValue;

	public RawDataOnTransit(RawDataAtNode rdNode) {
		rawDataID = rdNode.getRawDataID();
		niceName = rdNode.getNiceName();
		numberOfScans = rdNode.getNumberOfScans();
		scanTimes = rdNode.getScanTimes();
		minMZValue = rdNode.getMinMZValue();
		maxMZValue = rdNode.getMaxMZValue();
	}

}