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

package net.sf.mzmine.data;

import javax.annotation.Nonnull;

/**
 * This interface defines an isotope pattern which can be attached to a peak
 */
public interface IsotopePattern {

    /**
     * Returns the isotope pattern status.
     */
    @Nonnull
    public IsotopePatternStatus getStatus();

    /**
     * Returns the number of isotopes in this pattern
     */
    public int getNumberOfIsotopes();

    /**
     * Returns an array of m/z values and intensities of the isotopes. The size
     * of the array is same as returned by getNumberOfIsotopes()
     */
    @Nonnull
    public DataPoint[] getDataPoints();

    /**
     * Returns the highest (in terms of intensity) isotope of this pattern.
     */
    @Nonnull
    public DataPoint getHighestIsotope();

    /**
     * Returns a description of this isotope pattern (formula, etc.)
     */
    @Nonnull
    public String getDescription();

}