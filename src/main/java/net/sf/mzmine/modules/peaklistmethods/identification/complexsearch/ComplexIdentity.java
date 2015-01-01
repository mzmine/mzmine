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

package net.sf.mzmine.modules.peaklistmethods.identification.complexsearch;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;

public class ComplexIdentity extends SimplePeakIdentity {

    public ComplexIdentity(final PeakListRow peak1, final PeakListRow peak2) {

	super("Complex of "
		+ MZmineCore.getConfiguration().getMZFormat()
			.format(peak1.getAverageMZ())
		+ " and "
		+ MZmineCore.getConfiguration().getMZFormat()
			.format(peak2.getAverageMZ()) + " m/z");

	setPropertyValue(PROPERTY_METHOD, "Complex search");
    }
}
