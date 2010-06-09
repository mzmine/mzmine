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

package net.sf.mzmine.modules.peaklistmethods.identification.relatedpeaks;

import java.text.NumberFormat;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;

public class RelatedPeakIdentity implements PeakIdentity {

	private PeakListRow originalPeakListRow;
	private PeakListRow relatedPeakListRow;
	private String peakName;

	/**
	 * @param originalPeakListRow
	 * @param relatedPeakListRow
	 * @param adduct
	 */
	public RelatedPeakIdentity(PeakListRow originalPeakListRow,
			PeakListRow relatedPeakListRow) {

		this.originalPeakListRow = originalPeakListRow;
		this.relatedPeakListRow = relatedPeakListRow;

		NumberFormat mzFormat = MZmineCore.getMZFormat();

		// We have to save the copy of the name here. If we ask
		// originalPeakListRow.getName() everytime we are asked for a name, we
		// may create an infinite loop if two rows depend on each other
		this.peakName = "Peak related to "
				+ mzFormat.format(originalPeakListRow.getAverageMZ()) + " m/z";
	}

	/**
	 * @return Returns the identificationMethod
	 */
	public String getIdentificationMethod() {
		return "Related peak search";
	}

	/**
	 * @return Returns the ID
	 */
	public String getID() {
		return null;
	}

	/**
	 * @return Returns the Name
	 */
	public String getName() {
		return peakName;
	}

	/**
	 * @return Returns the originalPeakListRow
	 */
	public PeakListRow getOriginalPeakListRow() {
		return this.originalPeakListRow;
	}

	/**
	 * @return Returns the relatedPeakListRow
	 */
	public PeakListRow getRelatedPeakListRow() {
		return this.relatedPeakListRow;
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
