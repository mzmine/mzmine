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

package net.sf.mzmine.data;

/**
 * This interface represents an identification result.
 */
public interface CompoundIdentity {

    /**
     * Returns description of identification method, e.g. which database was
     * searched.
     * 
     * @return Identification method
     */
    public String getIdentificationMethod();

    /**
     * Returns ID of identified compound, e.g. ID of this compound in a given
     * database.
     * 
     * @return Compound ID
     */
    public String getCompoundID();

    /**
     * Returns short (a few characters) compound name, which can be displayed in
     * the visualizers.
     * 
     * @return Short compound name
     */
    public String getShortCompoundName();

    /**
     * Returns a full name of this compound
     * 
     * @return Full compound name
     */
    public String getFullCompoundName();

    /**
     * Returns alternate names of this compound
     * 
     * @return Array of alternate names
     */
    public String[] getAlternateNames();

    /**
     * Returns a relative degree of trust that the identified peak really
     * corresponds to this compound. Each identification method may use its own
     * scale of this value, therefore these values are only comparable if they
     * come from the same identification method.
     * 
     * @return Relative degree of trust that this identification is correct, in
     *         range 0.0 - 1.0
     */
    public float getCredibility();

    /**
     * Returns an URL for a WWW database entry covering this compound.
     * 
     * @return Database entry URL
     */
    public String getDatabaseEntryURL();

}
