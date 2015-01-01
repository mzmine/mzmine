/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;

public class AdductIdentity extends SimplePeakIdentity {

    /**
     * Create the identity.
     *
     * @param originalPeakListRow
     *            adduct of this peak list row.
     * @param adduct
     *            type of adduct.
     */
    public AdductIdentity(final PeakListRow originalPeakListRow,
	    final AdductType adduct) {

	super(adduct.toString()
		+ " adduct of "
		+ MZmineCore.getConfiguration().getMZFormat()
			.format(originalPeakListRow.getAverageMZ()) + " m/z");

	setPropertyValue(PROPERTY_METHOD, "Adduct search");
    }
}
