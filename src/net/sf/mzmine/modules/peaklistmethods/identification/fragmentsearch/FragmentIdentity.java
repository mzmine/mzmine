/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.fragmentsearch;

import java.text.NumberFormat;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;

public class FragmentIdentity implements PeakIdentity {

	private PeakListRow mainPeakListRow;
	private String fragmentName;

	/**
	 */
	public FragmentIdentity(PeakListRow mainPeakListRow,
			PeakListRow fragmentPeakListRow) {

		this.mainPeakListRow = mainPeakListRow;

		NumberFormat mzFormat = MZmineCore.getMZFormat();

		// We have to save the copy of the name here. If we ask
		// mainPeakListRow.getName() every time we are asked for a name, we may
		// create an infinite loop if two rows depend on each other
		this.fragmentName = "Fragment of "
				+ mzFormat.format(mainPeakListRow.getAverageMZ()) + " m/z";
	}

	/**
	 * @return Returns the identificationMethod
	 */
	public String getIdentificationMethod() {
		return "Fragment search";
	}

	/**
	 * @return Returns the Name
	 */
	public String getName() {
		return fragmentName;
	}

	public PeakListRow getMainPeak() {
		return mainPeakListRow;
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
