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

package net.sf.mzmine.modules.identification.peptidesearch.fileformats;

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
import net.sf.mzmine.modules.identification.peptidesearch.PeptideFileParser;
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
		
		HashMap section = sections.get("unimod");
		String xml = (String) section.get("XML");
		MascotXMLParser xmlParser = new MascotXMLParser(xml);
		sections.put("unimod", xmlParser.getMassesMap());
		
		section = sections.get("parameters");
		pepDataFile.setParameter("TOL", (String) section.get("TOL"));
		pepDataFile.setParameter("ITOL", (String) section.get("ITOL"));
		pepDataFile.setParameter("MASS", (String) section.get("MASS"));
		pepDataFile.setParameter("CLE", (String) section.get("CLE"));
		pepDataFile.setParameter("INSTRUMENT", (String) section.get("INSTRUMENT"));
		pepDataFile.setParameter("RULES", (String) section.get("RULES"));

		//MODS=Carbamidomethyl (C)
		String fixedModifications = (String) section.get("MODS");
		//IT_MODS=Deamidation (NQ),Oxidation (M)
		String variableModifications = (String) section.get("IT_MODS");
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
		HashMap section = sections.get("unimod");
		
		String element = null;
		Iterator it = section.keySet().iterator();
		while(it.hasNext()) { 
			element = (String) it.next(); 
			pepDataFile.setDefaultMass(element, (Double) section.get(element) ); 
		} 
		
		
	}

	public void parseQuery(int queryNumber,
			PeptideIdentityDataFile pepDataFile) {
		// Summary, peptides, proteins & queries 
		
	    //Peptide peptide;
	    Protein[] proteins;
	    double mass;
	    double massExpected;
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
		Iterator it = hitSequences.iterator ();
		section = sections.get("proteins");		
		while (it.hasNext ()) {
			//Get peptide info 
		    value = (String) it.next ();
		    
		    //Parse info and create peptide instances
		    Peptide[] pepsFromSequence = MascotParserUtils.parsePeptideInfo(queryNumber,value,mass,massExpected,precursorCharge,pepDataFile);
		    for (Peptide pep: pepsFromSequence){
			    
		    	// Set link of identified proteins and peptideIdentityDataFile
		    	proteins = pep.getProteins();
			    
			    for (int i=0;i<proteins.length; i++){
			    	if (proteins[i].getDescription() == null){
			    		proteins[i].setDescription((String) section.get("\""+proteins[i].getSysname()+"\""));
			    	}
			    	pepDataFile.addIdentifiedProtein(proteins[i].getSysname(), proteins[i]);
			    }

			    peptides.add(pep);
		    }

		}

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
		
		//Links the scan to the peptideIdentityFile
		pepDataFile.addPeptideScan(queryNumber, peptideScan);
		
		
	}

}
