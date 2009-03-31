/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.complexsearch;

import java.text.NumberFormat;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class ComplexIdentity implements PeakIdentity {

	private PeakListRow complexRow, peak1, peak2;
	private String complexName;

	/**
	 */
	public ComplexIdentity(PeakListRow complexRow, PeakListRow peak1,
			PeakListRow peak2) {

		this.complexRow = complexRow;
		this.peak1 = peak1;
		this.peak2 = peak2;

		NumberFormat mzFormat = MZmineCore.getMZFormat();

		// We have to save the copy of the name here. If we ask
		// mainPeakListRow.getName() every time we are asked for a name, we may
		// create an infinite loop if two rows depend on each other
		this.complexName = "Complex of "
				+ mzFormat.format(peak1.getAverageMZ()) + " and "
				+ mzFormat.format(peak2.getAverageMZ()) + " m/z";
	}

	/**
	 * @return Returns the identificationMethod
	 */
	public String getIdentificationMethod() {
		return "Complex search";
	}

	/**
	 * @return Returns the Name
	 */
	public String getName() {
		return complexName;
	}

	public PeakListRow getComplexPeak() {
		return complexRow;
	}

	public PeakListRow[] getComplexedPeaks() {
		return new PeakListRow[] { peak1, peak2 };
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}

	public String getDescription() {
		return getName();
	}

}
