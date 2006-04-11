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
import net.sf.mzmine.datastructures.RawDataAtClient;


public interface RawDataVisualizer {

	// Change types
	public static final int CHANGETYPE_NOCHANGE = 0;
	public static final int CHANGETYPE_CURSORPOSITION_SCAN = 1;
	public static final int CHANGETYPE_CURSORPOSITION_MZ = 2;
	public static final int CHANGETYPE_CURSORPOSITION_BOTH = 3;
	public static final int CHANGETYPE_SELECTION_SCAN = 11;
	public static final int CHANGETYPE_SELECTION_MZ = 12;
	public static final int CHANGETYPE_SELECTION_BOTH = 13;
	public static final int CHANGETYPE_DATA = 20;
	public static final int CHANGETYPE_PEAKS = 30;
	public static final int CHANGETYPE_JUSTOPENED  = 40;
	public static final int CHANGETYPE_PEAKSANDDATA = 50;
	public static final int CHANGETYPE_INTERNAL = 90;



	public void setRawData(RawDataAtClient _rawData);

	public RawDataAtClient getRawData();

	public RawDataVisualizerRefreshRequest beforeRefresh(RawDataVisualizerRefreshRequest refreshRequest);

	public void afterRefresh(RawDataVisualizerRefreshResult refreshResult);

	public void printMe();

	public void copyMe();



}