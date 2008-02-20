/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.identification.qbixlipiddb;

class QBIXLipidDBQuery {

	private String name;
	private float mz;			// TODO: Store both base peak m/z and monoisotopic m/z
	private float rt;
	private float tolerancePPM;
	private String adduct;
	private float add;
	private String expected;

	QBIXLipidDBQuery(String name, float mz, float rt, float tolerancePPM,
			String adduct, float add, String expected) {

		this.name = name;
		this.mz = mz;
		this.rt = rt;
		this.tolerancePPM = tolerancePPM;
		this.adduct = adduct;
		this.add = add;
		this.expected = expected;

	}

	String getName() {
		return name;
	}

	float getMZ() {
		return mz;
	}
	
	float getMinMZ() {
		return mz - mz * tolerancePPM / 1000000.0f;
	}

	float getMaxMZ() {
		return mz + mz * tolerancePPM / 1000000.0f;
	}

	float getRT() {
		return rt;
	}
	
	String getAdduct() {
		return adduct;
	}

	float getAdd() {
		return add;
	}

	String getExpected() {
		return expected;
	}

}
