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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;

public class AdductIdentity extends SimplePeakIdentity {

	private PeakListRow originalPeakListRow;
	private PeakListRow adductPeakListRow;
	private AdductType adduct;

	/**
	 * @param originalPeakListRow
	 * @param relatedPeakListRow
	 * @param adduct
	 */
	public AdductIdentity(PeakListRow originalPeakListRow,
			PeakListRow adductPeakListRow, AdductType adduct) {
		
		super(adduct.getName() + " adduct of "
		+ MZmineCore.getMZFormat().format(originalPeakListRow.getAverageMZ()) + " m/z");
		
		this.originalPeakListRow = originalPeakListRow;
		this.adductPeakListRow = adductPeakListRow;
		this.adduct = adduct;

		setPropertyValue(PROPERTY_METHOD, "Adduct search");

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
	public PeakListRow getAdductPeakListRow() {
		return this.adductPeakListRow;
	}

	/**
	 * @return Returns the type of adduct
	 */
	public AdductType getTypeOfAdduct() {
		return adduct;
	}

	/**
	 * @return Returns the mass difference
	 */
	public double getMassDifference() {
		return adduct.getMassDifference();
	}

}
