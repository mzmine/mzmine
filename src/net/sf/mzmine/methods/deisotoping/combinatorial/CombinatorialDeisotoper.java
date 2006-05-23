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
package net.sf.mzmine.methods.deisotoping.combinatorial;
import java.util.Vector;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


/**
 *	You can use this class for building the combinatorial deisotoping method
 */


public class CombinatorialDeisotoper implements Method {


	public String getMethodDescription() {
		return new String("Combinatorial deisotoper");
	}

	/**
	 * This method shows a dialog box to the user for manipulating default/previous parameter values
	 *
	 * @param	mainWin			MZmine main window (required for showing a modal dialog box)
	 * @param	currentValues	Previously used parameter values (null if there are no previous values)
	 */
	public boolean askParameters(MethodParameters parameters) {

		// If method's caller didn't give any previous parameter values, then initialize new parameters

		CombinatorialDeisotoperParameters currentParameters = (CombinatorialDeisotoperParameters)parameters;
		if (currentParameters==null) return false;


		// Since this method is only a stub, it directly returns the default parameters value object without
		// showing a dialog box to the user. When implementing & testing a new method, you can define
		// the required parameters as constants in class CombinatorialDeisotoperParameters. Later
		// we can add user-interface for manipulating those parameters

		return true;

	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {
		// TODO
	}

}

