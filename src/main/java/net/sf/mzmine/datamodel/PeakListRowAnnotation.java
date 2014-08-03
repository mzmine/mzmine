/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel;

import java.net.URL;

import javax.annotation.Nullable;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Annotation of a detected feature with a chemical structure, formula, or
 * textual description.
 */
public interface PeakListRowAnnotation {

    /**
     * @return Chemical structure of this annotation.
     */
    @Nullable
    IAtomContainer getStructure();

    /**
     * Sets a new chemical structure to this annotation.
     */
    void setStructure(@Nullable IAtomContainer structure);

    /**
     * @return Chemical formula of this annotation.
     * @see #setFormula(String)
     */
    @Nullable
    String getFormula();

    /**
     * Sets a new chemical structure annotation to this annotation. Chemical
     * formula annotation should include charge, e.g. C6H13O6+ or
     * [C34H58N5O35P3]2-.
     */
    void setFormula(@Nullable String formula);

    /**
     * @return Textual description of this annotation.
     */
    @Nullable
    String getDescription();

    /**
     * Sets a new description to this annotation.
     */
    void setDescription(@Nullable String description);

    /**
     * @return Identification method (e.g. database name) of this annotation.
     */
    @Nullable
    String getIdentificationMethod();

    /**
     * Sets a new identification method to this annotation.
     */
    void setIdentificationMethod(@Nullable String idMethod);

    /**
     * @return ID in a database, if this annotation comes from a database.
     */
    @Nullable
    String getDataBaseId();

    /**
     * Sets a new identification method to this annotation.
     */
    void setDataBaseId(@Nullable String dbId);

    /**
     * @return Accession URL for a database, if this annotation comes from a
     *         database.
     */
    @Nullable
    URL getAccessionURL();

    /**
     * Sets a new accession URL to this annotation.
     */
    void setAccessionURL(@Nullable URL dbURL);

}
