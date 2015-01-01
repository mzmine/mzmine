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

package net.sf.mzmine.util;

import java.util.Comparator;

import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Peptide;

public class PeptideSorter implements Comparator<Peptide> {

    private SortingDirection direction;

    public PeptideSorter(SortingDirection direction) {
	this.direction = direction;
    }

    public int compare(Peptide pep1, Peptide pep2) {

	Double pep1Value = pep1.getIonScore();
	Double pep2Value = pep2.getIonScore();

	if (direction == SortingDirection.Ascending)
	    return pep1Value.compareTo(pep2Value);
	else
	    return pep2Value.compareTo(pep1Value);

    }

}
