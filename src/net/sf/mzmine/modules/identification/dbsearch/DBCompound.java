/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.identification.dbsearch;

import java.net.URL;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;

public class DBCompound implements PeakIdentity {

	private OnlineDatabase searchedDB;
	private String compoundID, compoundName, compoundFormula;
	private URL databaseEntryURL, structure2DURL, structure3DURL;
	private double isotopePatternScore;
	private IsotopePattern isotopePattern;

	

	/**
	 * @param compoundID
	 * @param compoundName
	 * @param alternateNames
	 * @param compoundFormula
	 * @param databaseEntryURL
	 * @param identificationMethod
	 * @param scopeNote
	 */
	public DBCompound(OnlineDatabase searchedDB, String compoundID,
			String compoundName, String compoundFormula, URL databaseEntryURL,
			URL structure2DURL, URL structure3DURL) {

		this.searchedDB = searchedDB;
		this.compoundID = compoundID;
		this.compoundName = compoundName;
		this.compoundFormula = compoundFormula;
		this.databaseEntryURL = databaseEntryURL;
		this.structure2DURL = structure2DURL;
		this.structure3DURL = structure3DURL;

	}

	/**
	 * @return Returns the compoundFormula.
	 */
	public String getCompoundFormula() {
		return compoundFormula;
	}

	/**
	 * @return Returns the compoundID.
	 */
	public String getID() {
		return compoundID;
	}

	/**
	 * @return Returns the compound name
	 */
	public String getName() {
		return compoundName;
	}

	/**
	 * @return Returns the databaseEntryURL
	 */
	public URL getDatabaseEntryURL() {
		return databaseEntryURL;
	}

	/**
	 * @return Returns the 2D structure URL
	 */
	public URL get2DStructureURL() {
		return structure2DURL;
	}

	/**
	 * @return Returns the 3D structure URL
	 */
	public URL get3DStructureURL() {
		return structure3DURL;
	}

	/**
	 * @return Returns the identificationMethod.
	 */
	public String getIdentificationMethod() {
		return searchedDB.toString() + " search";
	}

	/**
	 * Set the isotope pattern (predicted) of this compound
	 * 
	 * @return String exact mass
	 */
	public void setIsotopePatternScore(double score) {
		this.isotopePatternScore = score;
	}

	/**
	 * Returns the isotope pattern (predicted) of this compound
	 * 
	 * @return IsotopePattern
	 */
	public double getIsotopePatternScore() {
		return isotopePatternScore;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return compoundName;
	}

	public String getDescription() {
		return compoundName + "\nFormula: " + compoundFormula + "\nID: "
				+ compoundID + "\nIdentification method: "
				+ getIdentificationMethod();
	}
	
	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}

	public void setIsotopePattern(IsotopePattern isotopePattern) {
		this.isotopePattern = isotopePattern;
	}
}
