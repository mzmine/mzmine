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
	private double exactMass, isotopePatternScore;
	private IsotopePattern isotopePattern;
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
			String compoundFormula, double exactMass) {

		if ((compoundName == null) || (compoundName.equals(""))) {
			if ((compoundFormula != null) && (compoundFormula.length() > 0)) {
				this.compoundName = compoundFormula;
			} else {
				this.compoundName = UNKNOWN_NAME;
			}
		} else {
			this.compoundName = compoundName;
		}

		this.compoundFormula = compoundFormula;
		this.compoundID = compoundID;
		this.exactMass = exactMass;
		this.databaseEntryURL = pubchemAddress + compoundID;

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
	public int getID() {
		return compoundID;
	}

	/**
	 * @return Returns the compoundName.
	 */
	public String getName() {
		return compoundName;
	}

	/**
	 * @return Returns the databaseEntryURL.
	 */
	public String getDatabaseEntryURL() {
		return databaseEntryURL;
	}

	/**
	 * @return Returns the identificationMethod.
	 */
	public String getIdentificationMethod() {
		return "PubChem compound database";
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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return compoundName;
	}

	public String getDescription() {
		return compoundName + "\nFormula: " + compoundFormula + "\nCID: "
				+ compoundID + "\nIdentification method: "
				+ getIdentificationMethod();
	}
}
