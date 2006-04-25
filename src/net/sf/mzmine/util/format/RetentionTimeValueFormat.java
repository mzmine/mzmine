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
package net.sf.mzmine.util.format;
import java.text.DecimalFormat;

import net.sf.mzmine.util.GeneralParameters;


/**
 * 
 */
public class RetentionTimeValueFormat implements ValueFormat {


    private static DecimalFormat rtFormat = new DecimalFormat("0.0");
    
    private int rtFormatSetting; // TODO:
    


	/**
	 * This function turns given rt value (in secs) to string representation (either "secs" or "mins:secs")
	 */
	public String format(double rt) {

		String val = null;

		if (rtFormatSetting == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC) {
			val = new String(rtFormat.format(rt));
		} else {
	
			int mins = (int)Math.floor(rt / 60);
			double secs = rt - mins*60;
			val = mins + ":" + rtFormat.format(secs);
		}

		return val;

	}


}