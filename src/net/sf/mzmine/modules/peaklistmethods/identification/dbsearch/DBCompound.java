/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimplePeakIdentity;

import java.net.URL;

/**
 * Peak identity as found by searching a compound database.
 *
 * @author $Author$
 * @version $Revision$
 */
public class DBCompound
        extends SimplePeakIdentity {
    private final URL structure2DURL;
    private final URL structure3DURL;
    private double isotopePatternScore;
    private IsotopePattern isotopePattern;

    /**
     * Create the identity.
     *
     * @param searchedDB       database searched.
     * @param compoundID       compound ID.
     * @param compoundName     common name.
     * @param compoundFormula  molecular formula.
     * @param databaseEntryURL database URL.
     * @param url2dStructure   2D structure URL.
     * @param url3dStructure   3D structure URL.
     */
    public DBCompound(final OnlineDatabase searchedDB,
                      final String compoundID,
                      final String compoundName,
                      final String compoundFormula,
                      final URL databaseEntryURL,
                      final URL url2dStructure,
                      final URL url3dStructure) {
        super(compoundName, compoundFormula, searchedDB + " search", compoundID, databaseEntryURL.toString());

        // Initialise.
        structure2DURL = url2dStructure;
        structure3DURL = url3dStructure;
        isotopePattern = null;
        isotopePatternScore = 0.0;
    }

    /**
     * Get the 2D structure URL.
     *
     * @return Returns the 2D structure URL
     */
    public URL get2DStructureURL() {
        return structure2DURL;
    }

    /**
     * Get the 3D structure URL.
     *
     * @return Returns the 3D structure URL
     */
    public URL get3DStructureURL() {
        return structure3DURL;
    }

    /**
     * Set the isotope pattern (predicted) of this compound.
     *
     * @param score the new score.
     */
    public void setIsotopePatternScore(final double score) {
        isotopePatternScore = score;
    }

    /**
     * Gets the isotope pattern (predicted) of this compound.
     *
     * @return IsotopePattern
     */
    public double getIsotopePatternScore() {
        return isotopePatternScore;
    }

    /**
     * Gets the isotope pattern.
     *
     * @return the pattern.
     */
    public IsotopePattern getIsotopePattern() {
        return isotopePattern;
    }

    /**
     * Sets the isotope pattern.
     *
     * @param pattern the pattern.
     */
    public void setIsotopePattern(final IsotopePattern pattern) {
        isotopePattern = pattern;
    }
}
