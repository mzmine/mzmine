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
import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MascotParserUtils;

public class PeptideIdentityDataFile {

    private Vector<RawDataFile> rawDataFiles;
    private String filename;
    private int numOfQueries;
    private HashMap<String, String> parameters;
    private Vector<ModificationPeptide> modifications;
    private HashMap<String, Protein> proteins;
    private HashMap<String, Double> defaultMasses;
    private Hashtable<Integer, PeptideScan> scans;
    private SerieIonType[] ionSeriesRules;
    private double peptideMassErrorTol = -1;
    private double fragmentIonMassErrorTol = -1;
    private double significanceThreshold;

    public PeptideIdentityDataFile(String filename) {
	this.filename = filename;
	this.parameters = new HashMap<String, String>();
	this.modifications = new Vector<ModificationPeptide>();
	this.defaultMasses = new HashMap<String, Double>();
	rawDataFiles = new Vector<RawDataFile>();
	scans = new Hashtable<Integer, PeptideScan>();
	proteins = new HashMap<String, Protein>();
    }

    /**
     * Sets the significance threshold to recognize a valid peptide's identity
     * 
     * @param significanceThreshold
     */
    public void setSignificanceThreshold(double significanceThreshold) {
	this.significanceThreshold = significanceThreshold;

    }

    /**
     * Returns the threshold value for valid peptide's score to assign protein's
     * identity
     * 
     * @return
     */
    public double getSignificanceThreshold() {
	return significanceThreshold;

    }

    /**
     * Returns the name of original data file
     */
    public String getName() {
	return filename;
    }

    /**
     * Change the name of this data file
     * 
     * @param String
     *            name
     */
    public void setName(String name) {
	this.filename = name;
    }

    /**
     * Returns the number of identified peptides
     */
    public int getNumOfPeptideQueries() {
	return numOfQueries;
    }

    /**
     * Sets the number of identified peptides
     * 
     * @param int queryNumber
     */
    public void setNumOfPeptideQueries(int numOfQueries) {
	this.numOfQueries = numOfQueries;
    }

    /**
     * Returns an array of RawDataFile linked to this file
     */
    public RawDataFile[] getRawDataFiles() {
	return rawDataFiles.toArray(new RawDataFile[0]);
    }

    /**
     * Add a RawDataFile instance where some of the scans came from.
     * 
     * @param RawDataFile
     *            rawFile
     */
    public void addRawDataFile(RawDataFile rawFile) {
	if (!CollectionUtils.arrayContains(
		rawDataFiles.toArray(new RawDataFile[0]), rawFile))
	    rawDataFiles.add(rawFile);
    }

    /**
     * Returns the parameters used in the identification of peptide's sequences.
     */
    public HashMap<String, String> getParameters() {
	return parameters;
    }

    /**
     * Sets the given parameter and its value.
     * 
     * @param String
     *            parameter
     * @param String
     *            value
     */
    public void setParameter(String parameter, String value) {
	this.parameters.put(parameter, value);
    }

    /**
     * Returns a Vector<ModificationPeptide>, containing the information about
     * the searched modifications on identified peptides.
     */
    public ModificationPeptide[] getSearchedModifications() {
	return modifications.toArray(new ModificationPeptide[0]);
    }

    /**
     * Adds a modification to the set of modifications searched.
     */
    public void addSearchedModification(ModificationPeptide modification) {
	modifications.add(modification);
    }

    /**
     * Returns the mass value for each element used to identify peptides
     */
    public HashMap<String, Double> getDefaultMasses() {
	return defaultMasses;
    }

    /**
     * Sets the mass value for an element used to identify peptides
     * 
     * @param String
     *            element
     * @param double mass
     */
    public void setDefaultMass(String element, double mass) {
	defaultMasses.put(element, mass);
    }

    /**
     * Returns a map, containing the information about the protein hits and
     * corresponding query (identified peptide).
     */
    public Protein[] getIdentifiedProteins() {
	return proteins.values().toArray(new Protein[0]);
    }

    /**
     * Returns a protein by its name
     * 
     * @param String
     *            proteinName
     */
    public Protein getProtein(String proteinName) {
	return proteins.get(proteinName);
    }

    /**
     * Adds a protein
     * 
     * @param String
     *            proteinName
     * @param Protein
     *            protein
     * 
     */
    public void addIdentifiedProtein(String proteinName, Protein protein) {
	if (!proteins.containsKey(proteinName))
	    proteins.put(proteinName, protein);
    }

    /**
     * Returns the PeptideScan according with the query number.
     * 
     * @param int queryNumber
     */
    public PeptideScan getPeptideScan(int queryNumber) {
	return scans.get(queryNumber);
    }

    /**
     * Returns the PeptideScan according with the query number.
     * 
     * @param int queryNumber
     */
    public void addPeptideScan(int queryNumber, PeptideScan scan) {
	scans.put(queryNumber, scan);
    }

    /**
     * Returns the Ion fragmentation rules applied.
     */
    public SerieIonType[] getIonSeriesRules() {
	if (ionSeriesRules == null)
	    ionSeriesRules = MascotParserUtils
		    .parseFragmentationRules((String) this.getParameters().get(
			    "RULES"));
	return ionSeriesRules;
    }

    /**
     * Returns the Ion fragmentation rules applied.
     */
    public double getPeptideMassErrorTolerance() {
	if (peptideMassErrorTol == -1)
	    peptideMassErrorTol = Double.parseDouble((String) this
		    .getParameters().get("TOL"));
	return peptideMassErrorTol;
    }

    /**
     * Returns the Ion fragmentation rules applied.
     */
    public double getFragmentIonMassErrorTolerance() {
	if (fragmentIonMassErrorTol == -1)
	    fragmentIonMassErrorTol = Double.parseDouble((String) this
		    .getParameters().get("ITOL"));
	return fragmentIonMassErrorTol;
    }

    /**
     * Close the file in case it is removed from the project
     */
    public void close() {

    }

}
