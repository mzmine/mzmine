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
package net.sf.mzmine.visualizers.rawdata;
import java.io.Serializable;

public class RawDataVisualizerRefreshResult implements Serializable {
	public int rawDataID;
	public int changeType;

	public int[] ticScanNumbers;
	public double[] ticIntensities;
	public double ticMaxIntensity;

	public double[] spectrumIntensities;
	public double spectrumMaxIntensity;
	public double[] spectrumMZValues;
	public double spectrumMinMZValue;
	public double spectrumMaxMZValue;
	public int spectrumCombinationStartScan;
	public int spectrumCombinationStopScan;


	public double[][] twodMatrix;
	public int twodMatrixWidth;
	public int twodMatrixHeight;
	public double twodMaxIntensity;
	public double twodDataMaxIntensity;
	public double twodMinIntensity;

}