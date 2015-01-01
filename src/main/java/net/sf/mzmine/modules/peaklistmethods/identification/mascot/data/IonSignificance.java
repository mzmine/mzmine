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

public enum IonSignificance {

    NOT_Sign_NOT_Scoring(0), // this as a random match
    Sign_NOT_Scoring(1), // fragment ion with significance but no importance for
			 // the score calculation of the peptide
    Sign_AND_Scoring(2);// fragment ion with significance and was used for the
			// score calculation of the peptide

    private int value;

    private IonSignificance(int value) {
	this.value = value;
    }

    public int getValue() {
	return value;
    }

}
