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

package net.sf.mzmine.data.proteomics;

import java.util.HashMap;

import net.sf.mzmine.util.ProteomeUtils;

public class Peptide {
	
	private int queryNumber;
	private String sequence;
	private float ionScore;
	private double mass;
	private double massExpected;
	private double precursorMass;
	private int precursorCharge;
	private int missedCleavages;
	private HashMap<Integer,ModificationPeptide> modifications;
	private PeptideIonSerie ionSerie;
	private PeptideScan scan;
	private PeptideFragmentation fragmentation;
	// Protein info
	private Protein protein;
	private int startRegion;
	private int stopRegion;
    private int multiplicity = 0;
	
	
	public Peptide(int queryNumber, String sequence, float ion_score,double mass, 
			double mass_expected, int charge, int missed, PeptideScan scan){
		this.queryNumber = queryNumber;
		this.sequence = sequence;
		this.ionScore = ion_score;
		this.mass = mass;
		this.massExpected = mass_expected;
		this.precursorCharge = charge;
		this.missedCleavages = missed;
		this.scan = scan;
	}

	/**
	 * Returns the amino acid sequence
	 */
	public int getQueryNumber(){
		return queryNumber;
	}
	
	/**
	 * Returns the amino acid sequence
	 */
	public String getSequence(){
		return sequence;
	}

	/**
	 * Returns the ion score 
	 */
	public float getIonScore(){
		return ionScore;
	}
	
	/**
	 * Returns the raw data mass of this peptide
	 */
	public double getMass(){
		return mass;
	}
	
	/**
	 * Returns the expected mass for this peptide
	 */
	public double getMass_expected(){
		return massExpected;
	}
	
	/**
	 * Returns the number of missed cleavages
	 */
	public int getMissedCleavages(){
		return missedCleavages;
	}
	
	/**
	 * Returns a HashMap<Integer,ModificationPeptide> with the modifications detected in this peptide
	 * and position
	 */
	public HashMap<Integer,ModificationPeptide> getModifications(){
		return modifications;
	}
	
	/**
	 * Returns the ion series
	 */
	public PeptideIonSerie getIonSeries(){
		return ionSerie;
	}
	
	/**
	 * @return Returns PeptideScan scan
	 */
	public PeptideScan getScan() {
		return scan;
	}
	
	/**
	 * Returns a precursor mass.
	 */
	public double getPrecursorMass() {
		return precursorMass;
	}

	public int getPrecursorCharge() {
		return precursorCharge;
	}
	
	public PeptideFragmentation getFragmentation(){
		return fragmentation;
	}
	
	public void setFragmentation(PeptideFragmentation fragmentation){
		this.fragmentation = fragmentation;
	}
	
	/**
	 * Sets a Vector<ModificationPeptide> with the modifications detected in this peptide
	 */
	public void setModifications(HashMap<Integer,ModificationPeptide> modifications){
		this.modifications = modifications;
	}
	
	/**
	 * Returns the amino acid sequence
	 */
	public void setIonSeries(PeptideIonSerie ionSerie){
		this.ionSerie = ionSerie;
	}
	
	/**
	 * Returns a description of this peptide
	 */
	public String toString() {
		return ProteomeUtils.peptideToString(this);
	}


	

}
