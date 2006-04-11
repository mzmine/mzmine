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
package net.sf.mzmine.methods.rawdata;
import net.sf.mzmine.obsoletedatastructures.RawDataAtNode;
import net.sf.mzmine.obsoletedistributionframework.NodeServer;


/**
 * Interface for all raw data filters
 *
 */
public interface Filter {


	/**
	 * This method displays a dialog for user to give parameters for filtering
	 * Method runs on a client
	 */
	//public FilterParameters askParameters(MainWindow mainWin, FilterParameters currentValues);


	/**
	 * This method does the filtering for given raw data file
	 * Method runs on a node
	 */
    public int doFiltering(NodeServer nodeServer, RawDataAtNode theData, FilterParameters filterParameters);

} // end Filter





