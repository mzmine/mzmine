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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

public class ProteinSection {

    private int startRegion;
    private int stopRegion;
    private int multiplicity;

    /**
     * This class represents the region of a protein's sequence that a peptide
     * covers.
     * 
     * @param startRegion
     * @param stopRegion
     * @param multiplicity
     */
    public ProteinSection(int startRegion, int stopRegion, int multiplicity) {
	this.startRegion = startRegion;
	this.stopRegion = stopRegion;
	this.multiplicity = multiplicity;
    }

    /**
     * Returns the initial position of the peptide coverage for this protein
     * 
     * @return
     */
    public int getStartRegion() {
	return startRegion;
    }

    /**
     * Returns the last position of the peptide coverage for this protein
     * 
     * @return
     */
    public int getStopRegion() {
	return stopRegion;
    }

    public int getMultiplicity() {
	return multiplicity;
    }

    public String getName() {
	return this.startRegion + " - " + this.stopRegion;
    }

}
