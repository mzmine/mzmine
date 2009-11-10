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
import java.util.Vector;

import net.sf.mzmine.util.ProteomeUtils;

public class Peptide {
	
	private int queryNumber;
	private String sequence;
	private float ionScore;
	private double mass;
	private double massExpected;
	private double precursorMass;
	private int precursorCharge;
	private double deltaMass;
	private int missedCleavages;
	private HashMap<Integer,ModificationPeptide> modifications;
	private PeptideIonSerie ionSerie;
	private PeptideScan scan;
	private PeptideFragmentation fragmentation;

	// Protein info
	private Vector<Protein> proteins;
    
    private String identificationMethod;
	
	
	public Peptide(int queryNumber, String sequence, float ion_score,double mass, 
			double massExpected, int charge, double precursorMass, double deltaMass, 
			int missed, PeptideScan scan, String identificationMethod){
		this.queryNumber = queryNumber;
		this.sequence = sequence;
		this.ionScore = ion_score;
		this.mass = mass;
		this.massExpected = massExpected;
		this.precursorCharge = charge;
		this.precursorMass = precursorMass;
		this.deltaMass = deltaMass;
		this.missedCleavages = missed;
		this.scan = scan;
		this.identificationMethod = identificationMethod;
		
		proteins = new Vector<Protein>();
	}

	/**
	 * Returns the number of query
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
	public double getMassExpected(){
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
	 * Sets the scan related with this peptide
	 * 
	 * @param peptideScan
	 */
	public void setScan(PeptideScan peptideScan) {
		this.scan = peptideScan;
	}

	
	/**
	 * Returns a precursor mass.
	 */
	public double getPrecursorMass() {
		return precursorMass;
	}
	
	/**
	 * Returns the delta mass or difference between the calculated mass and detected mass by the instrument
	 * 
	 * @return deltaMass
	 */
	public double getDeltaMass(){
		return deltaMass;
	}

	/**
	 * Returns the charge of the peptide, according with the parent scan.
	 * 
	 * @return charge
	 */
	public int getPrecursorCharge() {
		return precursorCharge;
	}
	
	/**
	 * Return the fragmentation of this peptide (B-ions, Y-ions, etc.)
	 * 
	 * @return fragmentation
	 */
	public PeptideFragmentation getFragmentation(){
		return fragmentation;
	}
	
	/**
	 * Set the fragmentation for this peptide
	 * 
	 * @param fragmentation
	 */
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
	 * Sets the Ion Series
	 */
	public void setIonSeries(PeptideIonSerie ionSerie){
		this.ionSerie = ionSerie;
	}
	
	/**
	 * Returns an array of Proteins that this peptide's sequence could fix into
	 * 
	 * @return proteins
	 */
	public Protein[] getProteins() {
		return proteins.toArray(new Protein[0]);
	}

	/**
	 * Add a protein to the group of that this peptide could belong.
	 * 
	 * @param protein
	 */
	public void addProtein(Protein protein) {
		this.proteins.add(protein);
	}
	
	/**
	 * Returns a description of how this data was generated (Mascot, etc.)
	 * 
	 * @return identificationMethod
	 */
	public String getIdentificationMethod() {
		return identificationMethod;
	}

	/**
	 * Sets the identification method
	 * 
	 * @param identificationMethod
	 */
	public void setIdentificationMethod(String identificationMethod) {
		this.identificationMethod = identificationMethod;
	}

	
	/**
	 * Returns a description of this peptide
	 */
	public String toString() {
		return ProteomeUtils.peptideToString(this);
	}

	

}
