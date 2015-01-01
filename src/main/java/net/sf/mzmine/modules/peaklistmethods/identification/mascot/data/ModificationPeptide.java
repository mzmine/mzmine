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

public class ModificationPeptide {

    private String name;
    private double mass;
    private boolean fixed;
    private char site;

    /**
     * This class represents a modification for any amino acid in the sequence
     * of a peptide.
     * 
     * @param name
     * @param mass
     * @param fixed
     */
    public ModificationPeptide(String name, double mass, char amino,
	    boolean fixed) {
	this.name = name;
	this.mass = mass;
	this.fixed = fixed;
    }

    /**
     * Returns the name of the modification
     * 
     * @return name
     */
    public String getName() {
	return name;
    }

    /**
     * Returns true if the modification is fixed (according with Mascot
     * definition)
     * 
     * @return boolean
     */
    public boolean isFixed() {
	return fixed;
    }

    /**
     * Return the mass value of this modification
     * 
     * @return mass
     */
    public double getMass() {
	return mass;
    }

    /**
     * Returns the site (amino acid) where this modification could happen.
     * 
     * @return char
     */
    public char getSite() {
	return site;
    }

}
