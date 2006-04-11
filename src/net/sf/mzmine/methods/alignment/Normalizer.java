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


package net.sf.mzmine.methods.alignment;

import net.sf.mzmine.datastructures.AlignmentResult;
import net.sf.mzmine.userinterface.MainWindow;

public interface Normalizer {


	/**
	 * This method displays a dialog for user to give parameters for peak picking
	 * Method runs on a client
	 */
	//public NormalizerParameters askParameters(MainWindow _mainWin);


	/**
	 * This method does the peak picking for given raw data file
	 * Method runs on a node
	 */
   	public AlignmentResult calcNormalization(MainWindow _mainWin, AlignmentResult alignmentResult, NormalizerParameters parameters);


}