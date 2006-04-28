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
package net.sf.mzmine.methods.filtering;
import java.text.NumberFormat;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class ZoomScanFilter implements Filter {

	private ZoomScanFilterParameters myParameters;

	private final String[] fieldNames = {
											"Minimum M/Z range width",
										};


	public ZoomScanFilterParameters askParameters(MainWindow mainWin, ZoomScanFilterParameters currentValues) {

		// Create filter parameter object
		ZoomScanFilterParameters tmpParameters;
		if (currentValues == null) {
			tmpParameters = new ZoomScanFilterParameters();
		} else {
			tmpParameters = currentValues;
		}


		// Show parameter setup dialog
		double[] paramValues = new double[1];
		paramValues[0] = tmpParameters.minMZRange;

		NumberFormat[] numberFormats = new NumberFormat[1];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);

		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);

		psd.showModal(mainWin.getDesktop());
/*
		psd.setLocationRelativeTo(mainWin);
		psd.setVisible(true);
*/

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return null;
		}

		// Read parameter values
		double d;

		// minMZRange
		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum M/Z range width!");
			return null;
		}
		tmpParameters.minMZRange = d;

		return tmpParameters;

	}


	public int doFiltering(RawDataFile rawData, FilterParameters _filterParameters) {

		ZoomScanFilterParameters filterParameters = (ZoomScanFilterParameters)_filterParameters;

		int outputNumberOfScans = -1;
		int outputNumberOfDatapoints = -1;
/*
		// Check if file type is NetCDF
		if (rawData.checkFileType()==RawDataFile.FILETYPE_NETCDF) {
			// Then we must count number of scans in the result file before filtering
			double[] mzMins = rawData.netdf_getScanMZRangeMins();
			double[] mzMaxs = rawData.netdf_getScanMZRangeMaxs();
			outputNumberOfScans=0;
			for (int ind=0; ind<mzMins.length; ind++) {
				if ((mzMaxs[ind]-mzMins[ind])>=filterParameters.minMZRange) { outputNumberOfScans++; }
			}
		}

		if (outputNumberOfScans==0) {
			return -1;
		}

		// Prepare for filtering input data and writing output data

		int ret = rawData.initializeForWriting(outputNumberOfDatapoints, outputNumberOfScans);
		if (ret != 1) { return ret; }

		int maxScan = rawData.getNumberOfScans();
		rawData.initializeScanBrowser(0, maxScan-1);


		// Loop through all scans

		for (int scani=0; scani<maxScan; scani++) {

			//nodeServer.updateJobCompletionRate((double)scani/(double)(maxScan-1));

			Scan sc = rawData.getNextScan();

			// Check if mz range is wide enough
			double mzMin = sc.getMZRangeMin();
			double mzMax = sc.getMZRangeMax();
			if ( (mzMax-mzMin)<filterParameters.minMZRange) { continue; }


			// Store scan
			ret = rawData.setScan(sc);

			if (ret != 1) {
				rawData.finalizeScanBrowser();
				rawData.finalizeAfterWriting();
				return ret;
			}

		}

		rawData.finalizeScanBrowser();

		ret = rawData.finalizeAfterWriting();

		return ret;*/
        return 0;

	}


}