/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.peptidesearch.fileformats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.mzmine.data.proteomics.ModificationPeptide;
import net.sf.mzmine.data.proteomics.Peptide;
import net.sf.mzmine.data.proteomics.PeptideIdentityDataFile;
import net.sf.mzmine.data.proteomics.PeptideScan;
import net.sf.mzmine.data.proteomics.Protein;
import net.sf.mzmine.modules.peaklistmethods.identification.peptidesearch.PeptideFileParser;
import net.sf.mzmine.util.MascotParserUtils;

public class MascotParser implements PeptideFileParser {

	private BufferedReader bufferReader;
	private int numOfQueries = -1;
	private HashMap<String,HashMap<String,Object>> sections;

	/**
	 * This class is a parser for Mascot result file (dat file), version 1.0.
	 *
	 * @param identityFile
	 * @throws Exception
	 */
	public MascotParser(File identityFile) throws Exception {
		FileInputStream fstream = new FileInputStream(identityFile);
		bufferReader = new BufferedReader(new InputStreamReader(fstream));
		parseReader();
	}

	public int getNumOfQueries() {
        if (numOfQueries < 0) {
        	numOfQueries = Integer.parseInt((String) sections.get("header").get("queries"));
        }
		return numOfQueries;
	}

    /**
     * This method is useful for parsing a result file from a Reader.
     */
    private void parseReader() throws IOException {

    	String line = null;
        if (bufferReader != null) {

        	// First line is to be ignored.
            line = bufferReader.readLine();

            // Find the boundary.
            line = bufferReader.readLine();
            while (line != null && line.indexOf("boundary") < 0) {
                line = bufferReader.readLine();
            }

            // Verify that exists separator 'boundary'
            if (line == null) {
                throw new IllegalArgumentException("Did not find 'boundary' definition in the datfile!");
            }
            String boundary = MascotParserUtils.getProperty(line, "boundary");

            boolean inSection = false;
            sections = new HashMap<String,HashMap<String,Object>>();
            StringBuffer sectionContents = null;
            String keySection = null;
            HashMap<String, Object> sectionMap = null;
            while ((line = bufferReader.readLine()) != null) {

                if (line.indexOf(boundary) >= 0) {

                    if (inSection) {
                    	sectionMap = MascotParserUtils.processSectionToHashMap(sectionContents.toString());
                        sections.put(keySection, sectionMap);
                    }

                    // Check for endmarker.
                    if (line.endsWith(boundary + "--")) {
                        break;
                    }

                    inSection = true;
                    keySection = MascotParserUtils.getProperty(bufferReader.readLine(), "name");
                    sectionContents = new StringBuffer();

                } else {
                    if (inSection) {
                        if (!line.trim().equals(""))
                            sectionContents.append(line + "\n");
                    }
                }

            }

        }

    }

	public void parseParameters(PeptideIdentityDataFile pepDataFile) {
		// Parameters, unimod, enzyme & header section

		HashMap<String, Object> section = sections.get("unimod");
		if (section != null){
			String xml = (String) section.get("XML");
			MascotXMLParser xmlParser = new MascotXMLParser(xml);
			sections.put("unimod", xmlParser.getMassesMap());
		}
		else {
			section = sections.get("masses");
			//delta1=16.003006,Oxidation (M)
			section = MascotParserUtils.parseModificationMass("delta", section);
			//FixedMod1=57.055400, Carbamidomethyl (C)
			section = MascotParserUtils.parseModificationMass("FixedMod", section);
			//Include terminal
			section.put("-",new String("0.0"));

			sections.put("unimod", section);

		}

		section = sections.get("parameters");
		pepDataFile.setParameter("TOL", (String) section.get("TOL"));
		pepDataFile.setParameter("ITOL", (String) section.get("ITOL"));
		pepDataFile.setParameter("MASS", (String) section.get("MASS"));
		pepDataFile.setParameter("CLE", (String) section.get("CLE"));
		pepDataFile.setParameter("INSTRUMENT", (String) section.get("INSTRUMENT"));
		pepDataFile.setParameter("RULES", (String) section.get("RULES"));

		//MODS=Carbamidomethyl (C)
		String fixedModifications = (String) section.get("MODS") != null ? (String) section.get("MODS") : "";
		//IT_MODS=Deamidation (NQ),Oxidation (M)
		String variableModifications = (String) section.get("IT_MODS") != null ? (String) section.get("IT_MODS") : "";
		section = sections.get("unimod");
		String token;
		//In case of many modifications
		StringTokenizer tokens = new StringTokenizer(fixedModifications,",");
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			ModificationPeptide[] singleMods = MascotParserUtils.parseModification(token,section,true);
			for (int i=0; i<singleMods.length; i++) {
					pepDataFile.addSearchedModification(singleMods[i]);
			}
		}

		tokens = new StringTokenizer(variableModifications,",");
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			ModificationPeptide[] singleMods = MascotParserUtils.parseModification(token,section,false);
			for (int i=0; i<singleMods.length; i++) {
					pepDataFile.addSearchedModification(singleMods[i]);
			}
		}

		pepDataFile.setNumOfPeptideQueries(this.getNumOfQueries());

	}


	public void parseDefaultMasses(PeptideIdentityDataFile pepDataFile) {
		// Masses & unimod section
		HashMap<String, Object> section = sections.get("unimod");

		String element = null;
		Iterator it = section.keySet().iterator();
		while(it.hasNext()) {
			element = (String) it.next();
			try{
				pepDataFile.setDefaultMass(element, Double.parseDouble((String) section.get(element) ) );
			}
			catch (Exception e){

			}
		}


	}

	public void parseQuery(int queryNumber,
			PeptideIdentityDataFile pepDataFile) {
		// Summary, peptides, proteins & queries

	    double mass;
	    double massExpected;
	    double identityScore;
	    double significanceThreshold = pepDataFile.getSignificanceThreshold();
	    int precursorCharge;
	    int count = 1;

		HashMap section = sections.get("summary");

		//Peptide mass
		String value = (String) section.get("qmass"+String.valueOf(queryNumber));
		mass = Double.parseDouble(value);

		//Detected mass and precursor charge
		//Example: "qexp100=381.700200,2+"
		value = (String) section.get("qexp"+String.valueOf(queryNumber));
		String[] tokens = value.split(",");
		massExpected = Double.parseDouble((String) tokens[0]);
		value = tokens[1].substring(0, tokens[1].indexOf("+"));
		precursorCharge = Integer.parseInt(value);

		//Identity and homology score
		//Example:
		//qmatch100=81 (identity)
		//qplughole100=25.351217 (homology)
		value = (String) section.get("qmatch"+String.valueOf(queryNumber));
		identityScore = Double.parseDouble((String) value);

		value = (String) section.get("qplughole"+String.valueOf(queryNumber));
		// homologyScore = Double.parseDouble((String) value);

		/*
		 * Calculates the identity score threshold value.
		 * This means the probability the relationship between
		 * the likelihood of random sequence and a true peptide's identity.
		*/
		double identityThreshold = MascotParserUtils.calculateIdentityThreshold(significanceThreshold,identityScore);

		value = null;
		section = sections.get("peptides");
		String key = "q"+String.valueOf(queryNumber)+"_p"+String.valueOf(count);
		Vector<String> hitSequences = new Vector<String>();

		while (section.containsKey(key)){
			value = (String) section.get(key);
			//No peptide sequence asigned
			if (value.equalsIgnoreCase("-1"))
				break;

			key += "_terms";
			value += ";"+(String) section.get(key);
			hitSequences.add(value);
			count++;
			key = "q"+String.valueOf(queryNumber)+"_p"+String.valueOf(count);
			value = null;
		}

		if (hitSequences.size() == 0)
			return;

		Vector<Peptide> peptides = new Vector<Peptide>();
		Vector<Peptide> alterPeptides = new Vector<Peptide>();
		Iterator it = hitSequences.iterator ();
		section = sections.get("proteins");
	    Protein protein;

	    boolean onlyHighScore = true;
	    boolean highScoreAdded = false;

	    while (it.hasNext ()) {
			//Get peptide info
		    value = (String) it.next ();

		    //Parse info and create peptide instances
		    Peptide[] pepsFromSequence = MascotParserUtils.parsePeptideInfo(queryNumber,value,
		    		mass,massExpected,precursorCharge,pepDataFile,identityThreshold,onlyHighScore);

		    for (Peptide pep: pepsFromSequence){

		    	// Set link of identified proteins and peptideIdentityDataFile
		    	protein = pep.getProtein();
		    	if (protein.getDescription() == null){
		    		protein.setDescription((String) section.get("\""+protein.getSysname()+"\""));
		    	}

		    	//Just register as identified protein the one that has highest score peptide
		    	//This works based on Mascot results are already sorted by score (descending).
		    	if (onlyHighScore){
		    		pepDataFile.addIdentifiedProtein(protein.getSysname(), protein);
		    		peptides.add(pep);
		    		highScoreAdded = true;
		    	}
		    	else
		    		alterPeptides.add(pep);
		    }

		    if (highScoreAdded)
		    	onlyHighScore = false;


		}

	    if (peptides.size() == 0){
	    	//logger.info(" No peptides pass filter in query "+queryNumber);
	    	return;
	    }

    	//logger.info(" Pass filter "+peptides.size()+" peptides in query "+queryNumber);

		//Retrieve info of data points of each peptide (MS/MS data point values)
		section = sections.get("query"+String.valueOf(queryNumber));

		PeptideScan peptideScan = MascotParserUtils.parseScanIons(queryNumber, section, pepDataFile);
		peptideScan.setPrecursorMZ(massExpected);

		it = peptides.iterator();
		Peptide peptide;
		while (it.hasNext()){
			peptide = (Peptide) it.next();
			//Link peptide and peptideScan
			peptide.setScan(peptideScan);
			peptideScan.addPeptide(peptide);
		}

		peptideScan.setAlterPeptides(alterPeptides.toArray(new Peptide[0]));

		//Links the scan to the peptideIdentityFile
		pepDataFile.addPeptideScan(queryNumber, peptideScan);


	}

}
