/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.util.Hashtable;
import java.util.ArrayList;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.DataUnit;


/**
 *
 */
public class GrouperIsotopePattern implements IsotopePattern {

	private int chargeState;

	// This is for implementing DataUnit interface
	private Hashtable<Class, ArrayList<DataUnit>> myDataUnits;


	public GrouperIsotopePattern(int chargeState) {
		this.chargeState = chargeState;
	}

	/**
	 * Returns the charge state of peaks in the pattern
	 */
	public int getChargeState() {
		return chargeState;
	}


	/* These methods implement the DataUnit interface */

	public void addData(Class dataType, DataUnit data) {

		ArrayList<DataUnit> correctSet = myDataUnits.get(dataType);

		if (correctSet==null) {
			correctSet = new ArrayList<DataUnit>();
			myDataUnits.put(dataType, correctSet);
		}

		correctSet.add(data);

	}

	public DataUnit[] getData(Class dataType) {

		ArrayList<DataUnit> adu = myDataUnits.get(dataType);

		if (adu==null) return new DataUnit[0];

		return myDataUnits.get(dataType).toArray(new DataUnit[0]);

	}

	public boolean hasData(Class dataType) {

		ArrayList<DataUnit> adu = myDataUnits.get(dataType);

		return adu!=null;

	}




}