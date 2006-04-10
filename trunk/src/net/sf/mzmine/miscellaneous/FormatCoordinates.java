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
package net.sf.mzmine.miscellaneous;
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
import java.text.DecimalFormat;


/**
 * This class is used for converting m/z, time and scan numbers to
 * their textual representation according to current settings.
 */
public class FormatCoordinates {

	// Helper objects that do the actual formatting
    private DecimalFormat mzFormat;
    private DecimalFormat rtFormat;

    // These store the current formatting settings
    private Integer mzFormatSetting;
    private Integer rtFormatSetting;


	/**
	 * Constructor
	 *
	 */
	public FormatCoordinates(GeneralParameters param) {
		setMZValueFormat(param.getMZLabelFormat());
		setRTValueFormat(param.getChromatographicLabelFormat());
	}

	/**
	 * This function changes current representation of M/Z values
	 */
	public void setMZValueFormat(Integer format) {

		mzFormatSetting = format;
		if (format == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_TWODIGITS) 	{ mzFormat = new DecimalFormat("0.00"); }
		if (format == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_THREEDIGITS) 	{ mzFormat = new DecimalFormat("0.000"); }
		if (format == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_FOURDIGITS) 	{ mzFormat = new DecimalFormat("0.0000"); }

	}


	/**
	 * This function changes current representation of RT values
	 */
	public void setRTValueFormat(Integer format) {

		rtFormatSetting = format;
		if (format == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC) 		{ rtFormat = new DecimalFormat("0.0"); }
		if (format == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC) 	{ rtFormat = new DecimalFormat("00"); }

	}


	/**
	 * This function turns given M/Z value into string representation with currently selected number of decimals
	 */
	public String formatMZValue(double mz) {

		return new String(mzFormat.format(mz));

	}

	/**
	 * This function turns given rt value (in secs) to string representation (either "secs" or "mins:secs")
	 */
	public String formatRTValue(double rt) {

		String val = null;

		if (rtFormatSetting == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC) {
			val = new String(rtFormat.format(rt));
		}

		if (rtFormatSetting == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC) {
			int mins = (int)java.lang.Math.floor(rt / 60);
			double secs = rt - mins*60;
			val = mins + ":" + rtFormat.format(secs);
		}

		return val;

	}


	/**
	 * This function translates scan number into rt value string
	 */
	public String formatRTValue(int scanNum, RawDataAtClient r) {

		String val = null;

		if (rtFormatSetting == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC) {
			val = new String(rtFormat.format(r.getScanTime(scanNum)));
		}

		if (rtFormatSetting == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC) {
			double tot_secs = r.getScanTime(scanNum);
			int mins = (int)java.lang.Math.floor(tot_secs / 60);
			double secs = tot_secs - mins*60;
			val = mins + ":" + rtFormat.format(secs);
		}

		return val;
	}


}