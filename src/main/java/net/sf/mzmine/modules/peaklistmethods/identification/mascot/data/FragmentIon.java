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

import net.sf.mzmine.util.ProteomeUtils;

public class FragmentIon {

    private double mass;
    private FragmentIonType ionType;
    private int position;

    /**
     * This class represents a fragment ion comming from a sequence of amino
     * acids (peptide)
     * 
     * @param mass
     * @param fragmentIonType
     * @param position
     */
    public FragmentIon(double mass, FragmentIonType fragmentIonType,
	    int position) {
	this.mass = mass;
	this.ionType = fragmentIonType;
    }

    /**
     * Returns the mass value for this fragment ion
     * 
     * @return mass
     */
    public double getMass() {
	return mass;
    }

    /**
     * Returns the enum FragmentIonType assigned to this fragment ion
     * 
     * @return ionType
     */
    public FragmentIonType getType() {
	return ionType;
    }

    /**
     * Returns the position of this fragment (Example: b2 where b is the type
     * and 2 is the position)
     * 
     * @return position
     */
    public int getPosition() {
	return position;
    }

    public String getName() {
	return ProteomeUtils.fragmentIonToString(this);
    }

}
