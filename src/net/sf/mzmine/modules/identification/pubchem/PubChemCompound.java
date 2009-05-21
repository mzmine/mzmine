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

package net.sf.mzmine.modules.identification.pubchem;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;

public class PubChemCompound implements PeakIdentity {

	private int compoundID;
	private String compoundName, compoundFormula, databaseEntryURL;
	private double exactMass;
	private String isotopePatternScore = "";
	private IsotopePattern isotopePattern;
	private String structure;
	public static final String pubchemAddress = "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=";
	
	static final String UNKNOWN_NAME = "Unknown name";

	/**
	 * @param compoundID
	 * @param compoundName
	 * @param alternateNames
	 * @param compoundFormula
	 * @param databaseEntryURL
	 * @param identificationMethod
	 * @param scopeNote
	 */
	public PubChemCompound(int compoundID, String compoundName,
			String compoundFormula, String databaseEntryURL) {
		this.compoundName = compoundName;
		this.compoundFormula = compoundFormula;

		if ((compoundName == null) || (compoundName.equals(""))) {
			this.compoundName = UNKNOWN_NAME;
		}

		this.compoundID = compoundID;
		if (databaseEntryURL == null) {
			this.databaseEntryURL = pubchemAddress + compoundID;
		} else {
			this.databaseEntryURL = databaseEntryURL;
		}

	}

	/**
	 * @return Returns the compoundFormula.
	 */
	public String getCompoundFormula() {
		return compoundFormula;
	}

	/**
	 * @param compoundFormula
	 *            The compoundFormula to set.
	 */
	public void setCompoundFormula(String compoundFormula) {
		this.compoundFormula = compoundFormula;
	}

	/**
	 * @return Returns the compoundID.
	 */
	public int getID() {
		return compoundID;
	}

	/**
	 * @param compoundID
	 *            The compoundID to set.
	 */
	public void setCompoundID(int compoundID) {
		this.compoundID = compoundID;
	}

	/**
	 * @return Returns the compoundName.
	 */
	public String getName() {
		return compoundName;
	}

	/**
	 * @param compoundName
	 *            The compoundName to set.
	 */
	public void setCompoundName(String compoundName) {
		this.compoundName = compoundName;
	}

	/**
	 * @return Returns the databaseEntryURL.
	 */
	public String getDatabaseEntryURL() {
		return databaseEntryURL;
	}

	/**
	 * @param databaseEntryURL
	 *            The databaseEntryURL to set.
	 */
	public void setDatabaseEntryURL(String databaseEntryURL) {
		this.databaseEntryURL = databaseEntryURL;
	}

	/**
	 * @return Returns the identificationMethod.
	 */
	public String getIdentificationMethod() {
		return "PubChem compound database";
	}

	/**
	 * Set the difference between this compound and the detected peak
	 * 
	 * @return String exact mass
	 */
	public void setExactMass(double exactMass) {
		this.exactMass = exactMass;
	}

	/**
	 * Returns the difference between this compound and the detected peak
	 * 
	 * @return String exact mass
	 */
	public double getExactMass() {
		return exactMass;
	}

	/**
	 * Set the isotope pattern (predicted) of this compound
	 * 
	 * @return String exact mass
	 */
	public void setIsotopePatternScore(String score) {
		this.isotopePatternScore = score;
	}

	/**
	 * Returns the isotope pattern (predicted) of this compound
	 * 
	 * @return IsotopePattern
	 */
	public String getIsotopePatternScore() {
		return isotopePatternScore;
	}

	/**
	 * Returns the isotope pattern of this compound identity.
	 * 
	 * @return isotopePattern
	 */
	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}

	/**
	 * Assign an isotope pattern to this compound identity.
	 * 
	 * @param isotopePattern
	 */
	public void setIsotopePattern(IsotopePattern isotopePattern) {
		this.isotopePattern = isotopePattern;
	}

	/**
	 * Assign the structure (SDF format) of this compound.
	 * 
	 * @param structure
	 */
	public void setStructure(String structure) {
		this.structure = structure;
	}

	/**
	 * Returns the structure (SDF format) of this compound.
	 * 
	 * @return structure
	 */
	public String getStructure() {
		return structure;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getDescription();
	}

	public String getDescription() {
		return compoundName + " (" + compoundFormula + ")\n" + "CID"
				+ compoundID + "\n" + "Identification method: "
				+ getIdentificationMethod();
	}
}
