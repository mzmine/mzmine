/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import java.net.URL;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimplePeakIdentity;

public class DBCompound extends SimplePeakIdentity {

    private URL structure2DURL, structure3DURL;
    private Double isotopePatternScore;
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

	super(compoundName, compoundFormula, searchedDB + " search",
		compoundID, databaseEntryURL.toString());

	this.structure2DURL = structure2DURL;
	this.structure3DURL = structure3DURL;
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
     * Set the isotope pattern (predicted) of this compound
     * 
     * @return String exact mass
     */
    public void setIsotopePatternScore(double score) {
	isotopePatternScore = score;
    }

    /**
     * Returns the isotope pattern score or null if the score was not calculated
     * 
     * @return IsotopePattern
     */
    public Double getIsotopePatternScore() {
	return isotopePatternScore;
    }

    /**
     * Returns the isotope pattern (predicted) of this compound
     * 
     * @return IsotopePattern
     */
    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }
}
