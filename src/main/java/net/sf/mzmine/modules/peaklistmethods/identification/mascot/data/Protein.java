/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

import java.util.HashMap;
import java.util.Vector;

import net.sf.mzmine.util.ProteomeUtils;

public class Protein {

    private String sysname;
    private int hits;
    private HashMap<String, ProteinSection> coverage;
    private Vector<Peptide> peptides;
    private Vector<Peptide> alterPeptides;
    private String description = null;

    /**
     * This class represents a protein, which is identified by many peptides.
     * Also this class keeps information about the section of sequence that
     * cover each peptide and the number of hits (scan number) related to this
     * protein.
     * 
     * @param String
     *            protein name
     */
    public Protein(String sysname) {
	this.sysname = sysname;
	coverage = new HashMap<String, ProteinSection>();
	peptides = new Vector<Peptide>();
	alterPeptides = new Vector<Peptide>();
    }

    /**
     * Returns the protein name (sysname)
     * 
     * @return String name
     */
    public String getSysname() {
	return sysname;
    }

    /**
     * Adds a peptide to this protein with the section of protein's sequence
     * that covers
     * 
     * @param peptide
     * @param section
     * @param isTopScore
     */
    public void addPeptide(Peptide peptide, ProteinSection section,
	    boolean isTopScore) {

	if (isTopScore) {
	    if (!coverage.containsKey(peptide.getSequence())) {
		coverage.put(peptide.getSequence(), section);
	    }
	    peptides.add(peptide);
	    hits++;
	} else {
	    alterPeptides.add(peptide);
	}
    }

    /**
     * Returns all the peptides associated with this protein
     * 
     * @return Peptide[]
     */
    public Peptide[] getPeptides() {
	return peptides.toArray(new Peptide[0]);
    }

    /**
     * Returns the section of protein's sequence information that corresponds
     * with the peptide as argument.
     * 
     * @param peptide
     * @return ProteinSection
     */
    public ProteinSection getSection(Peptide peptide) {
	return coverage.get(peptide.getSequence());
    }

    /**
     * Returns the number of peptides associated to this protein
     * 
     * @return int peptide number
     */
    public int getPeptidesNumber() {
	return coverage.size();
    }

    /**
     * Returns the number of hits (scans) associated to this protein
     * 
     * @return int hits
     */
    public int getHits() {
	return hits;
    }

    /**
     * Sets the number of hits for this protein
     * 
     * @param hits
     */
    public void setHits(int hits) {
	this.hits = hits;
    }

    /**
     * Returns a description of this protein
     * 
     * @return String description
     */
    public String getDescription() {
	return description;
    }

    /**
     * Sets the description for this protein
     * 
     * @param description
     */
    public void setDescription(String description) {
	this.description = description;
    }

    public String getName() {
	return ProteomeUtils.proteinToString(this);
    }

}
