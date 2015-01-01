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

package net.sf.mzmine.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.IonSignificance;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.ModificationPeptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Peptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideFragmentation;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideIdentityDataFile;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideIonSerie;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideScan;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Protein;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.ProteinSection;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.SerieIonType;

public class MascotParserUtils {

    /*
     * FRAGMENT ION/RULE KEYS: 1 singly charged 2 doubly charged if precursor 2+
     * or higher (not internal or immonium) 3 doubly charged if precursor 3+ or
     * higher (not internal or immonium) 4 immonium 5 a series 6 a - NH3 if a
     * significant and fragment includes RKNQ 7 a - H2O if a significant and
     * fragment includes STED 8 b series 9 b - NH3 if b significant and fragment
     * includes RKNQ 10 b - H2O if b significant and fragment includes STED 11 c
     * series 12 x series 13 y series 14 y - NH3 if y significant and fragment
     * includes RKNQ 15 y - H2O if y significant and fragment includes STED 16 z
     * series 17 internal yb < 700 Da 18 internal ya < 700 Da 19 y or y++ must
     * be significant 20 y or y++ must be highest scoring series 21 z+1 series
     * 22 d and d' series 23 v series 24 w and w' series 25 z+2 series
     * 
     * INSTRUMENT RULES: Default = 1,2,8,9,10,13,14,15 ESI-QUAD-TOF =
     * 1,2,8,9,10,13,14,15 MALDI-TOF-PSD = 1,4,5,6,7,8,9,10,13 ESI-TRAP =
     * 1,2,8,9,10,13,14,15 ESI-QUAD = 1,2,8,9,10,13,14,15 ESI-FTICR =
     * 1,2,8,9,10,13,14,15 MALDI-TOF-TOF =
     * 1,4,5,6,7,8,9,10,13,14,15,17,18,22,23,24 ESI-4SECTOR =
     * 1,2,4,5,8,9,10,13,16,17,18 FTMS-ECD = 1,2,11,13,21,25 ETD-TRAP =
     * 1,2,11,13,21,25 MALDI-QUAD-TOF = 1,2,4,89,10,13,14,15,17,18 MALDI-QIT-TOF
     * = 1,4,5,6,78,9,10,13,14,15,17,18
     */
    public static SerieIonType[] parseFragmentationRules(String sRules) {
	Vector<SerieIonType> ionSeries = new Vector<SerieIonType>();
	StringTokenizer st = new StringTokenizer(sRules, ",");
	int ionRule = 0;
	while (st.hasMoreTokens()) {
	    ionRule = Integer.parseInt(st.nextToken());
	    switch (ionRule) {
	    case 5: // a-ion
		ionSeries.add(SerieIonType.A_SERIES);
		break;

	    case 8: // b-ion
		ionSeries.add(SerieIonType.B_SERIES);
		break;

	    case 11: // c-ion
		ionSeries.add(SerieIonType.C_SERIES);
		break;

	    case 12: // x-ion
		ionSeries.add(SerieIonType.X_SERIES);
		break;

	    case 13: // y-ion
		ionSeries.add(SerieIonType.Y_SERIES);
		break;

	    case 16: // z-ion
		ionSeries.add(SerieIonType.Z_SERIES);
		break;

	    case 21: // zH-ion
		ionSeries.add(SerieIonType.ZH_SERIES);
		break;

	    case 25: // zHH-ion
		ionSeries.add(SerieIonType.ZHH_SERIES);
		break;

	    default:
		break;
	    }
	}

	return ionSeries.toArray(new SerieIonType[0]);
    }

    /*
     * ion series
     * 
     * [Mascot 2.2] 0 a 1 place holder 2 a++
     * 
     * 3 b 4 place holder 5 b++
     * 
     * 6 y 7 place holder 8 y++
     * 
     * 9 c 10 c++ 11 x 12 x++ 13 z 14 z++ 15 z+H 16 z+H++ 17 z+2H 18 z+2H++
     * 
     * 0 0 0 0 0 0 0 0 0 0 1 0 0 2 0 0 2 0 1 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14
     * 15 16 17 18
     */
    public static PeptideIonSerie parseIonSeriesSignificance(
	    String seriesIonString) {

	PeptideIonSerie peptideIonSeries = new PeptideIonSerie();
	IonSignificance significance = IonSignificance.NOT_Sign_NOT_Scoring;
	for (int i = 0; i < seriesIonString.length(); i++) {

	    significance = IonSignificance.NOT_Sign_NOT_Scoring;
	    switch (seriesIonString.charAt(i)) {
	    case '1':
		significance = IonSignificance.NOT_Sign_NOT_Scoring;
		break;

	    case '2':
		significance = IonSignificance.NOT_Sign_NOT_Scoring;
		break;
	    }

	    switch (i) {
	    case 0: // a-ion
		peptideIonSeries.addSerie(SerieIonType.A_SERIES, significance);
		break;
	    case 2: // a-double-ion
		peptideIonSeries.addSerie(SerieIonType.A_DOUBLE_SERIES,
			significance);
		break;

	    case 3: // b-ion
		peptideIonSeries.addSerie(SerieIonType.B_SERIES, significance);
		break;

	    case 5: // b-double-ion
		peptideIonSeries.addSerie(SerieIonType.B_DOUBLE_SERIES,
			significance);
		break;

	    case 6: // y-ion
		peptideIonSeries.addSerie(SerieIonType.Y_SERIES, significance);
		break;

	    case 8: // y-ion
		peptideIonSeries.addSerie(SerieIonType.Y_DOUBLE_SERIES,
			significance);
		break;

	    case 9: // c-ion
		peptideIonSeries.addSerie(SerieIonType.C_SERIES, significance);
		break;

	    case 10: // c-double-ion
		peptideIonSeries.addSerie(SerieIonType.C_DOUBLE_SERIES,
			significance);
		break;

	    case 11: // x-ion
		peptideIonSeries.addSerie(SerieIonType.X_SERIES, significance);
		break;

	    case 12: // x-double-ion
		peptideIonSeries.addSerie(SerieIonType.X_DOUBLE_SERIES,
			significance);
		break;

	    case 13: // z-ion
		peptideIonSeries.addSerie(SerieIonType.Z_SERIES, significance);
		break;

	    case 14: // z-double-ion
		peptideIonSeries.addSerie(SerieIonType.Z_DOUBLE_SERIES,
			significance);
		break;

	    case 15: // zh-ion
		peptideIonSeries.addSerie(SerieIonType.ZH_SERIES, significance);
		break;

	    case 16: // zh-double-ion
		peptideIonSeries.addSerie(SerieIonType.ZH_DOUBLE_SERIES,
			significance);
		break;

	    case 17: // zhh-ion
		peptideIonSeries
			.addSerie(SerieIonType.ZHH_SERIES, significance);
		break;

	    case 18: // zhh-double-ion
		peptideIonSeries.addSerie(SerieIonType.ZHH_DOUBLE_SERIES,
			significance);
		break;

	    default:
		break;
	    }
	}

	return peptideIonSeries;
    }

    /**
     * This method finds a property, associated by a name.
     *
     * @param line
     *            String with the line on which the 'KEY=VALUE' pair is to be
     *            found.
     * @param propName
     *            String with the name of the KEY.
     * @return String property
     */
    public static String getProperty(String line, String propName) {
	propName += "=";
	int start = line.indexOf(propName);
	int offset = propName.length();
	// System.out.println("---" + line);
	String found = line.substring(start + offset).trim();
	// Trim away opening and closing '"'.
	if (found.startsWith("\"")) {
	    found = found.substring(1);
	}
	if (found.endsWith("\"")) {
	    found = found.substring(0, found.length() - 1);
	}
	return found.trim();
    }

    /**
     * This method parses the content of a section into key-value pairs and
     * stores these in a HashMap.
     *
     * @param aContent
     *            the content of the section to be parsed.
     * @return HashMap with the contents of the section as key-value pairs.
     */
    public static HashMap<String, Object> processSectionToHashMap(
	    String aContent) {

	HashMap<String, Object> sectionMap = new HashMap<String, Object>();
	try {
	    BufferedReader bufferReader = new BufferedReader(new StringReader(
		    aContent));
	    String line = null;
	    boolean firstLine = true;
	    while ((line = bufferReader.readLine()) != null) {
		// KEY=VALUE
		// If this section has XML content (such as the modification /
		// quantitation sections as of Mascot 2.2)
		// The xml section is stored as a whole in the map for the key
		// 'XML'.
		if (firstLine) {
		    if (line.startsWith("<?xml version")) {
			// We match a xml section.
			sectionMap.put("XML", aContent); // Now break the while
							 // loop.//
			break;
		    }
		    firstLine = false;
		}

		// More, for header lines ('hx_text'; holding the protein
		// description)
		// or Protein section names (starting with the accession,
		// encircled by '"')
		// we need to make sure we have the full description line (it
		// might
		// contain an '=', as IPI headers often do)!
		String key = null;
		Object value = null;
		if ((line.startsWith("h") && line.indexOf("_text=") >= 0)
			|| (line.startsWith("\"") && line.endsWith("\""))) {
		    key = line.substring(0, line.indexOf("=")).trim();
		    if (line.length() > line.indexOf("=") + 1) {
			value = line.substring(line.indexOf("=") + 1).trim();
		    }
		} else {
		    StringTokenizer lst = new StringTokenizer(line, "=");
		    key = lst.nextToken().trim();
		    value = null;
		    if (lst.hasMoreTokens()) {
			value = lst.nextToken().trim();
		    }
		}
		sectionMap.put(key, value);
	    }
	    bufferReader.close();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
	return sectionMap;
    }

    /*
     * String "info" is divided into three sections by ";", which the first one
     * is peptide, second protein and third is peptide terminals.
     * 
     * Peptide section must have 11 elements: missed cleavages, peptide mass,
     * delta mass, number of matched ions, aa sequence, peaks used from ion 1,
     * variable modifications (binary code), ion score, ion series, peaks used
     * from ions2, peaks used from ion 3
     * 
     * Protein section must have 5 elements: accesion name, frame number, start,
     * stop and multiplicity
     * 
     * Peptide terminal section must have 2 elements; left aa, right aa of the
     * sequence
     * 
     * Example='
     * 1,743.428955,0.000893,5,KGAAQLR,25,000001000,27.19,0002001000000000000
     * ,0,0;
     * "SPBC839.04":0:23:29:1,"SPBC2F12.07c":0:23:29:1,"SPAC1F7.13c":0:23:29:1;
     * R,T:R,T:R,T '
     */
    public static Peptide[] parsePeptideInfo(int queryNumber, String info,
	    double mass, double massExpected, int precursorCharge,
	    PeptideIdentityDataFile pepDataFile, double identityThreshold,
	    boolean isTopScore) {

	String[] tokens = info.split(";");
	String peptideSection = tokens[0];
	String proteinSection = tokens[1];
	String terminalSection = tokens[2];

	// Terminals
	tokens = terminalSection.split(":");
	String[] peptideInfos = new String[tokens.length];
	for (int i = 0; i < tokens.length; i++) {
	    peptideInfos[i] = peptideSection + "," + tokens[i];
	}

	// Proteins
	tokens = proteinSection.split(",");
	String[] proteinInfos = new String[tokens.length];
	for (int i = 0; i < tokens.length; i++) {
	    proteinInfos[i] = tokens[i];
	}

	if (proteinInfos.length != peptideInfos.length)
	    return null;
	Vector<Peptide> peptides = new Vector<Peptide>();

	// Peptide
	for (int pepIndex = 0; pepIndex < peptideInfos.length; pepIndex++) {

	    tokens = peptideInfos[pepIndex].split(",");
	    int missedCleavages = Integer.parseInt(tokens[0]);
	    double precursorMass = Double.parseDouble(tokens[1]);
	    double deltaMass = Double.parseDouble(tokens[2]);
	    String sequence = tokens[11] + tokens[4] + tokens[12];
	    String modSeries = tokens[6];
	    double ionScore = Double.parseDouble(tokens[7]);

	    if (ionScore >= identityThreshold)
		continue;

	    Peptide peptide = new Peptide(queryNumber, sequence, ionScore,
		    mass, massExpected, precursorCharge, precursorMass,
		    deltaMass, missedCleavages, null, "Mascot", isTopScore);

	    HashMap<Integer, ModificationPeptide> modifications = new HashMap<Integer, ModificationPeptide>();
	    ModificationPeptide[] searchedMods = pepDataFile
		    .getSearchedModifications();

	    // TODO Verify the possibility of two modifications in the same
	    // site.
	    for (int pos = 0; pos < modSeries.length(); pos++) {
		if (modSeries.charAt(pos) == '1') {
		    char aa = sequence.charAt(pos);
		    for (int index = 0; index < searchedMods.length; index++) {
			if (searchedMods[index].getSite() == aa) {
			    modifications.put(pos, searchedMods[index]);
			}
		    }
		}
	    }

	    peptide.setModifications(modifications);

	    // Ion serie
	    PeptideIonSerie peptideIonSerie = parseIonSeriesSignificance(tokens[8]);
	    peptide.setIonSeries(peptideIonSerie);

	    // Calculate fragmentation
	    PeptideFragmentation fragmentation = new PeptideFragmentation(
		    peptide, pepDataFile);
	    peptide.setFragmentation(fragmentation);

	    // Protein info
	    ProteinSection section;
	    tokens = proteinInfos[pepIndex].split(":");
	    String sysname = tokens[0].replace("\"", "");

	    Protein protein = pepDataFile.getProtein(sysname);
	    if (protein == null)
		protein = new Protein(sysname);

	    int startRegion = Integer.parseInt(tokens[2]);
	    int stopRegion = Integer.parseInt(tokens[3]);
	    int multiplicity = Integer.parseInt(tokens[4]);
	    section = new ProteinSection(startRegion, stopRegion, multiplicity);

	    // Link peptide and protein
	    peptide.setProtein(protein);
	    protein.addPeptide(peptide, section, isTopScore);

	    peptides.add(peptide);

	}

	return peptides.toArray(new Peptide[0]);
    }

    /**
     * Parse a string with information of possible modifications in a peptide's
     * amino acid sequence
     * 
     * @param modString
     * @param section
     * @param fixed
     * @return
     */
    public static ModificationPeptide[] parseModification(String modString,
	    HashMap<?, ?> section, boolean fixed) {
	// Example "Deamidation (NQ)"
	Vector<ModificationPeptide> mods = new Vector<ModificationPeptide>();
	double mass = 0.0;
	modString = modString.replace("\n", "");
	String[] tokens = modString.split(" ");
	String name = tokens[0];
	String sites = tokens[1];
	if (sites.endsWith(")")) {
	    sites = sites.replace("(", "");
	    sites = sites.replace(")", "");
	    for (int i = 0; i < sites.length(); i++) {
		mass = Double.parseDouble((String) section.get(name));
		mods.add(new ModificationPeptide(name, mass, sites.charAt(i),
			fixed));
	    }
	}
	return mods.toArray(new ModificationPeptide[0]);
    }

    /**
     * Parse the information of data points (MS/MS peaks) and generate a new
     * PeptideScan
     * 
     * @param queryNumber
     * @param HashMap
     *            sectionMap with the data points info
     * @param pepDataFile
     * @return
     */
    public static PeptideScan parseScanIons(int queryNumber,
	    HashMap<?, ?> sectionMap, PeptideIdentityDataFile pepDataFile) {

	String titleScan = (String) sectionMap.get("title");
	titleScan = titleScan.replace("%2e", ",");
	String[] tokens = titleScan.split(",");
	String rawFileName = tokens[0];
	int rawScanNumber = Integer.parseInt(tokens[1]);

	PeptideScan scan = new PeptideScan(pepDataFile, rawFileName,
		queryNumber, rawScanNumber);
	Vector<SimpleDataPoint> dataPoints = new Vector<SimpleDataPoint>();
	double mass, intensity;
	String ions = (String) sectionMap.get("Ions1");
	StringTokenizer tokenizer = new StringTokenizer(ions, ",");
	while (tokenizer.hasMoreTokens()) {
	    tokens = tokenizer.nextToken().split(":");
	    mass = Double.parseDouble(tokens[0]);
	    intensity = Double.parseDouble(tokens[1]);
	    dataPoints.add(new SimpleDataPoint(mass, intensity));
	}

	scan.setDataPoints(dataPoints.toArray(new SimpleDataPoint[0]));

	return scan;
    }

    /**
     * Compare two arrays of DataPoint and return true if they are equal
     * 
     */
    public static boolean compareDataPointsByMass(DataPoint[] dataPoints,
	    DataPoint[] dataPoints2) {

	boolean flag = true;

	Integer[] masses1 = new Integer[dataPoints.length];
	for (int i = 0; i < dataPoints.length; i++)
	    masses1[i] = (int) dataPoints[i].getMZ();

	Integer[] masses2 = new Integer[dataPoints2.length];
	for (int i = 0; i < dataPoints2.length; i++)
	    masses2[i] = (int) dataPoints2[i].getMZ();

	for (int i = 0; i < masses2.length; i++) {
	    if (!CollectionUtils.arrayContains(masses1, masses2[i]))
		flag = false;
	}

	return flag;

    }

    public static HashMap<String, Object> parseModificationMass(String keyPart,
	    HashMap<String, Object> section) {

	int count = 1;
	String key = keyPart + count;
	String value;

	while (section.containsKey(key)) {
	    value = (String) section.get(key);
	    // No variable modifications
	    if (value == null)
		break;
	    if (value.equalsIgnoreCase("-1"))
		break;

	    // Parse mass
	    String[] tokens = value.split(",");
	    String mass = tokens[0];
	    // Oxidation (M)
	    int endIndex = tokens[1].lastIndexOf(" ");
	    value = tokens[1].substring(0, endIndex);
	    value = value.trim();
	    section.remove(key);
	    section.put(value, mass);
	    count++;
	    key = keyPart + count;
	    value = null;
	}

	return section;
    }

    public static double calculateIdentityThreshold(
	    double significanceThreshold, double identityQueryScore) {

	/*
	 * This formula comes from Mascot software.
	 */
	double identityThreshold = identityQueryScore
		/ (significanceThreshold * 20);
	identityThreshold = Math.log(identityThreshold) * 10.0;

	return identityThreshold;
    }

}
