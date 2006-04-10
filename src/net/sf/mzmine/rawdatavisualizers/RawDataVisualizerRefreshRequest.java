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
package net.sf.mzmine.rawdatavisualizers;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


// Java packages
import java.io.Serializable;

public class RawDataVisualizerRefreshRequest implements Serializable {
	// Possible values for ticMode
	public static final int MODE_TIC = 1;
	public static final int MODE_XIC = 2;

	// Possible values for spectrumMode
	public static final int MODE_SINGLESPECTRUM = 3;
	public static final int MODE_COMBINEDSPECTRA = 4;

	// Possible values for dataType
	public static final int MODE_CONTINUOUS = 5;
	public static final int MODE_CENTROIDS = 6;


	public int rawDataID;

	public int changeType;

	public int dataType;

	public boolean ticNeedsRawData = false;
	public int ticMode;
	public int ticStartScan, ticStopScan;
	public double ticStartMZ, ticStopMZ;

	public boolean spectrumNeedsRawData = false;
	public int spectrumMode;
	public int spectrumXResolution;
	public int spectrumStartScan, spectrumStopScan;
	public double spectrumStartMZ, spectrumStopMZ;

	public boolean twodNeedsRawData = false;
	public int twodXResolution, twodYResolution;
	public int twodScansPerX;
	public int twodStartScan, twodStopScan;
	public double twodStartMZ, twodStopMZ;

	public boolean peakListNeedsRawData = false;


}