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

import java.util.HashMap;
import java.util.Vector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.FragmentIon;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.FragmentIonType;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.ModificationPeptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Peptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideFragmentation;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.PeptideIdentityDataFile;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.SerieIonType;

public class PeptideUtils {

    /**
     * This method calculates the start position of a possible H2O loss for the
     * B-ion series. The returning variable is used for the H2O-loss
     * fragmentions calculation. This variable holds the smallest index of
     * S|T|E|D amino acid residue. These aminoacids can loose their H2O.
     *
     * @param String
     *            sequence
     * @return int index.
     */
    public static int getFirstBionLossH2O(String sequence) {
	int indexBionLossH2O = sequence.length() - 1;
	if (sequence.indexOf('S') != -1
		&& sequence.indexOf('S') < indexBionLossH2O) {
	    indexBionLossH2O = sequence.indexOf('S');
	}
	if (sequence.indexOf('T') != -1
		&& sequence.indexOf('T') < indexBionLossH2O) {
	    indexBionLossH2O = sequence.indexOf('T');
	}
	if (sequence.indexOf('D') != -1
		&& sequence.indexOf('D') < indexBionLossH2O) {
	    indexBionLossH2O = sequence.indexOf('D');
	}
	if (sequence.indexOf('E') != -1
		&& sequence.indexOf('E') < indexBionLossH2O) {
	    indexBionLossH2O = sequence.indexOf('E');
	}
	return indexBionLossH2O;
    }

    /**
     * This method calculates the start position of a possible H2O loss for the
     * Y-ion series. The returning variable is used for the H2O-loss
     * fragmentions calculation. This variable holds the smallest index of
     * S|T|E|D amino acid residue. These aminoacids can loose their H2O.
     *
     * @param String
     *            sequence
     * @return int index.
     */
    public static int getFirstYionLossH2O(String sequence) {
	int indexYionLossH2O = 0;
	if (sequence.indexOf('S') != -1
		&& sequence.indexOf('S') > indexYionLossH2O) {
	    indexYionLossH2O = sequence.indexOf('S');
	}
	if (sequence.indexOf('T') != -1
		&& sequence.indexOf('T') > indexYionLossH2O) {
	    indexYionLossH2O = sequence.indexOf('T');
	}
	if (sequence.indexOf('D') != -1
		&& sequence.indexOf('D') > indexYionLossH2O) {
	    indexYionLossH2O = sequence.indexOf('D');
	}
	if (sequence.indexOf('E') != -1
		&& sequence.indexOf('E') > indexYionLossH2O) {
	    indexYionLossH2O = sequence.indexOf('E');
	}
	return indexYionLossH2O;
    }

    /**
     * This method calculates the start position of a possible NH3 loss for the
     * B-ion series. The returning variable is used for the NH3-loss
     * fragmentions calculation. This variable holds the smallest index of
     * R|K|N|Q amino acid residue. These amino acids can loose their NH3.
     *
     * @param String
     *            sequence
     * @return int index.
     */
    public static int getFirstBionLossNH3(String sequence) {
	int indexBionLossNH3 = sequence.length() - 1;
	if (sequence.indexOf('R') != -1
		&& sequence.indexOf('R') < indexBionLossNH3) {
	    indexBionLossNH3 = sequence.indexOf('R');
	}
	if (sequence.indexOf('K') != -1
		&& sequence.indexOf('K') < indexBionLossNH3) {
	    indexBionLossNH3 = sequence.indexOf('K');
	}
	if (sequence.indexOf('N') != -1
		&& sequence.indexOf('N') < indexBionLossNH3) {
	    indexBionLossNH3 = sequence.indexOf('N');
	}
	if (sequence.indexOf('Q') != -1
		&& sequence.indexOf('Q') < indexBionLossNH3) {
	    indexBionLossNH3 = sequence.indexOf('Q');
	}
	return indexBionLossNH3;
    }

    /**
     * This method calculates the start position of a possible NH3 loss for the
     * Y-ion series. The returning variable is used for the NH3-loss
     * fragmentions calculation. This variable holds the smallest index of
     * R|K|N|Q amino acid residue. These aminoacids can loose their NH3.
     *
     * @param String
     *            sequence
     * @return int index.
     */
    public static int getFirstYionLossNH3(String sequence) {
	int indexYionLossNH3 = 0;
	if (sequence.indexOf('R') != -1
		&& sequence.indexOf('R') > indexYionLossNH3) {
	    indexYionLossNH3 = sequence.indexOf('R');
	}
	if (sequence.indexOf('K') != -1
		&& sequence.indexOf('K') > indexYionLossNH3) {
	    indexYionLossNH3 = sequence.indexOf('K');
	}
	if (sequence.indexOf('N') != -1
		&& sequence.indexOf('N') > indexYionLossNH3) {
	    indexYionLossNH3 = sequence.indexOf('N');
	}
	if (sequence.indexOf('Q') != -1
		&& sequence.indexOf('Q') > indexYionLossNH3) {
	    indexYionLossNH3 = sequence.indexOf('Q');
	}
	return indexYionLossNH3;
    }

    /**
     * This method calculates fragment ions b and y.
     */
    public static double[] calculatePeptideMasses(
	    HashMap<String, Double> defaultMasses, String sequence,
	    HashMap<Integer, ModificationPeptide> modifications) {

	int length = sequence.length();
	String aminoKey;
	// Calculated the mass of each amino acid including detected
	// modification
	double[] peptideMasses = new double[length];
	for (int i = 0; i < length; i++) {
	    // This double will hold the mass of the peptideUnit.
	    double aminoMass = 0.0;

	    /*
	     * Check if there is a N-Terminal modification, if there is one,
	     * count the mass of the N-terminal modification to the mass of the
	     * first amino acid.
	     */
	    if (i == 0) {
		if (modifications.containsKey(i))
		    aminoMass += modifications.get(i).getMass();
	    }
	    /*
	     * Check if there is a C-Terminal modification, if there is one,
	     * count the mass of the C-terminal modification to the mass of the
	     * last amino acid.
	     */
	    if (i == length - 1) {
		if (modifications.containsKey(i + 2))
		    aminoMass += modifications.get(i + 2).getMass();
	    }
	    // For aa i, count its mass to the UnitMass.
	    aminoKey = Character.toString(sequence.charAt(i));
	    aminoMass = aminoMass + defaultMasses.get(aminoKey);
	    /*
	     * If there is a modification on aa i, count the mass at the
	     * UnitMass. if the modification is fixed , don't add the mass
	     * because Mascot already included an equivalent modified amino acid
	     * mass.
	     */
	    if (modifications.containsKey(i + 1)) {
		ModificationPeptide mod = modifications.get(i + 1);
		if (mod.isFixed())
		    aminoMass += mod.getMass();
	    }

	    peptideMasses[i] = aminoMass;
	}
	return peptideMasses;

    }

    /**
     * This method calculates fragment ions b.
     */
    public static Vector<FragmentIon> calculateBions(double[] peptideMasses,
	    double hydrogenMass) {

	Vector<FragmentIon> bIons = new Vector<FragmentIon>();

	for (int i = 1; i < peptideMasses.length; i++) {
	    double bMass = 0.0;

	    /*
	     * Sum all the peptides from left until the current position.
	     * Finally add 1 extra hydrogen on the N-terminal AA if there is no
	     * fixed N-terminal modification. A B-ion has no new components at
	     * its C-terminal end.
	     */
	    for (int j = 0; j < i; j++) {
		bMass += peptideMasses[j];
	    }
	    bMass = bMass + hydrogenMass;

	    bIons.add(new FragmentIon(bMass, FragmentIonType.B_ION, i));

	}

	return bIons;
    }

    /**
     * This method calculates fragment ions y.
     */
    public static Vector<FragmentIon> calculateYions(double[] peptideMasses,
	    double hydrogenMass, double oxygenMass) {

	Vector<FragmentIon> yIons = new Vector<FragmentIon>();
	double ctermMass = hydrogenMass + oxygenMass;

	for (int i = 1; i < peptideMasses.length; i++) {
	    double yMass = 0.0;

	    /*
	     * Sum all the peptides from right until the current position.
	     * Finally add 1 extra hydroxyl on the C-terminal AA. A Y-ion also
	     * has 2 extra Hydrogens on its N-terminal end (NH in peptide bond,
	     * NH3+ when its free).
	     */
	    for (int j = 0; j < i; j++) {
		yMass += peptideMasses[(peptideMasses.length - 1) - j];
	    }
	    yMass = yMass + ctermMass + (2 * hydrogenMass);
	    yIons.add(new FragmentIon(yMass, FragmentIonType.Y_ION, i));
	}

	return yIons;
    }

    /**
     * Return b-ions plus 'CO' minus 'H2' (a-ions).
     *
     * @return Vector<FragmentIon> aIons.
     */
    public static Vector<FragmentIon> calculateAions(FragmentIon[] bIons,
	    double carbonMass, double oxygenMass, double hydrogenMass) {
	Vector<FragmentIon> aIons = new Vector<FragmentIon>();
	double mass;
	double CO = carbonMass + oxygenMass;
	double H2 = hydrogenMass * 2;

	for (int i = 0; i < bIons.length; i++) {
	    // b-ion minus 'CO' plus 'H2'
	    mass = bIons[i].getMass() - CO + H2;
	    aIons.add(new FragmentIon(mass, FragmentIonType.A_ION, (i + 1)));
	}
	return aIons;
    }

    /**
     * Return b-ions plus 'NH3' (c-ions).
     *
     * @return Vector<FragmentIon> cIons.
     */
    public static Vector<FragmentIon> calculateCions(FragmentIon[] bIons,
	    double nitrogenMass, double hydrogenMass) {
	Vector<FragmentIon> cIons = new Vector<FragmentIon>();
	double mass;
	double NH3 = nitrogenMass + (hydrogenMass * 3);

	for (int i = 0; i < bIons.length; i++) {
	    // b-ion plus 'NH3'
	    mass = bIons[i].getMass() + NH3;
	    cIons.add(new FragmentIon(mass, FragmentIonType.C_ION, (i + 1)));
	}
	return cIons;
    }

    /**
     * Return y-ion plus 'CO', minus 'H2' (x-ions).
     *
     * @return Vector<FragmentIon> zIons.
     */
    public static Vector<FragmentIon> calculateXions(FragmentIon[] yIons,
	    double carbonMass, double oxygenMass, double hydrogenMass) {
	Vector<FragmentIon> xIons = new Vector<FragmentIon>();
	double mass;
	double CO = carbonMass + oxygenMass;
	double H2 = hydrogenMass * 2;

	for (int i = 0; i < yIons.length; i++) {
	    // y-ion minus 'NH3' plus 'H2'
	    mass = yIons[i].getMass() + CO - H2;
	    xIons.add(new FragmentIon(mass, FragmentIonType.X_ION, (i + 1)));
	}
	return xIons;
    }

    /**
     * Return y-ions minus 'NH3' (z-ions).
     *
     * @return Vector<FragmentIon> zIons.
     */
    public static Vector<FragmentIon> calculateZions(FragmentIon[] bIons,
	    double nitrogenMass, double hydrogenMass) {
	Vector<FragmentIon> cIons = new Vector<FragmentIon>();
	double mass;
	double NH3 = nitrogenMass + (hydrogenMass * 3);

	for (int i = 0; i < bIons.length; i++) {
	    // y-ion minus 'NH3'
	    mass = bIons[i].getMass() - NH3;
	    cIons.add(new FragmentIon(mass, FragmentIonType.Z_ION, (i + 1)));
	}
	return cIons;
    }

    /**
     * Return z-ions plus 'H' (zh-ions).
     *
     * @return Vector<FragmentIon> zhIons.
     */
    public static Vector<FragmentIon> calculateZHions(FragmentIon[] zIons,
	    double hydrogenMass) {
	Vector<FragmentIon> zhIons = new Vector<FragmentIon>();
	double mass;

	for (int i = 0; i < zIons.length; i++) {
	    // b-ion minus 'CO' plus 'H2'
	    mass = zIons[i].getMass() + hydrogenMass;
	    zhIons.add(new FragmentIon(mass, FragmentIonType.ZH_ION, (i + 1)));
	}
	return zhIons;
    }

    /**
     * Return z-ions plus 'H2' (zhh-ions).
     *
     * @return Vector<FragmentIon> zhhIons.
     */
    public static Vector<FragmentIon> calculateZHHions(FragmentIon[] zIons,
	    double hydrogenMass) {
	Vector<FragmentIon> zhhIons = new Vector<FragmentIon>();
	double mass;

	for (int i = 0; i < zIons.length; i++) {
	    // z-ion plus 'H2'
	    mass = zIons[i].getMass() + (hydrogenMass * 2);
	    zhhIons.add(new FragmentIon(mass, FragmentIonType.ZHH_ION, (i + 1)));
	}
	return zhhIons;
    }

    /**
     * Returns all double charged ions.
     *
     * @return Vector<FragmentIon> doubleChargedIons
     */
    public static Vector<FragmentIon> calculateDoubleChargedIons(
	    FragmentIon[] bIons, double hydrogenMass, FragmentIonType ionType) {
	Vector<FragmentIon> bDoubleIons = new Vector<FragmentIon>();
	double mass;
	for (int i = 0; i < bIons.length; i++) {
	    // b-ion plus 'H' over a double charge
	    mass = (bIons[i].getMass() + hydrogenMass) / 2.0;
	    bDoubleIons.add(new FragmentIon(mass, ionType, i + 1));
	}
	return bDoubleIons;
    }

    /**
     * Returns Ions minus 'H2O', single and double charge.
     *
     * @return Vector<FragmentIon> ions - H2O
     */
    public static Vector<FragmentIon> calculateIonsLossH2O(FragmentIon[] bIons,
	    double hydrogenMass, double oxygenMass, int indexFirstIonLossH2O,
	    boolean doubleCharged, FragmentIonType ionType) {
	Vector<FragmentIon> bIonsLossH20 = new Vector<FragmentIon>();
	double lossMass;
	if (doubleCharged)
	    lossMass = hydrogenMass + oxygenMass;
	else
	    lossMass = (hydrogenMass * 2) + oxygenMass;

	double mass;

	// Verify position of fist aa who can loose H2O
	if (indexFirstIonLossH2O != bIons.length) {
	    for (int i = indexFirstIonLossH2O; i < bIons.length; i++) {
		// b-ion minus 'H2O'
		mass = bIons[i].getMass() - lossMass;
		if (doubleCharged)
		    mass /= 2.0;
		bIonsLossH20.add(new FragmentIon(mass, ionType, i + 1));
	    }
	}
	return bIonsLossH20;
    }

    /**
     * Returns Ions minus 'NH3', single and double charge.
     *
     * @return Vector<FragmentIon> ions - NH3
     */
    public static Vector<FragmentIon> calculateIonsLossNH3(FragmentIon[] bIons,
	    double hydrogenMass, double nitrogenMass, int indexFirstIonLossNH3,
	    boolean doubleCharged, FragmentIonType ionType) {
	Vector<FragmentIon> bIonsLossH20 = new Vector<FragmentIon>();
	double lossMass;
	if (doubleCharged)
	    lossMass = nitrogenMass + (hydrogenMass * 2);
	else
	    lossMass = nitrogenMass + (hydrogenMass * 3);
	double mass;

	// The peptide must be checked for position of the first amino acid who
	// losses NH3
	if (indexFirstIonLossNH3 != bIons.length) {
	    for (int i = indexFirstIonLossNH3; i < bIons.length; i++) {
		// b-ion minus 'NH3'
		mass = bIons[i].getMass() - lossMass;
		if (doubleCharged)
		    mass /= 2.0;
		bIonsLossH20.add(new FragmentIon(mass, ionType, i + 1));
	    }
	}
	return bIonsLossH20;
    }

    /**
     * Returns precursor ions , minus 'NH3' and minus H2O.
     *
     * @return Vector<FragmentIon> precursorIons
     */
    public static FragmentIon[] calculatePrecursorIons(double precursorMass,
	    int charge, double hydrogenMass, double oxygenMass,
	    double nitrogenMass) {
	Vector<FragmentIon> precursorIons = new Vector<FragmentIon>();
	double H2O = (hydrogenMass) * 2 + oxygenMass;
	double NH3 = nitrogenMass + (hydrogenMass * 3);
	if (precursorMass != 0.0) {
	    precursorIons.add(new FragmentIon(precursorMass,
		    FragmentIonType.PRECURSOR, 0));
	    double mass = precursorMass - (H2O / charge);
	    precursorIons.add(new FragmentIon(mass,
		    FragmentIonType.PRECURSOR_H2O, 0));
	    mass = precursorMass - (NH3 / charge);
	    precursorIons.add(new FragmentIon(mass,
		    FragmentIonType.PRECURSOR_NH3, 0));
	}
	return precursorIons.toArray(new FragmentIon[0]);
    }

    /**
     * Returns immonium ions
     *
     * @return Vector<FragmentIon> immoniumIons
     */
    public static FragmentIon[] calculateImmoniumIons(String sequence,
	    HashMap<String, Double> defaultMasses) {
	Vector<FragmentIon> immoniumIons = new Vector<FragmentIon>();

	for (int i = 0; i < sequence.length(); i++) {
	    char c = sequence.charAt(i);
	    switch (c) {
	    case 'A':
		immoniumIons.add(new FragmentIon(defaultMasses.get("A"),
			FragmentIonType.IMMONIUM_A, 0));
		break;
	    case 'R':
		immoniumIons.add(new FragmentIon(defaultMasses.get("R"),
			FragmentIonType.IMMONIUM_R, 0));
		break;
	    case 'N':
		immoniumIons.add(new FragmentIon(defaultMasses.get("N"),
			FragmentIonType.IMMONIUM_N, 0));
		break;
	    case 'D':
		immoniumIons.add(new FragmentIon(defaultMasses.get("D"),
			FragmentIonType.IMMONIUM_D, 0));
		break;
	    case 'C':
		immoniumIons.add(new FragmentIon(defaultMasses.get("C"),
			FragmentIonType.IMMONIUM_C, 0));
		break;
	    case 'E':
		immoniumIons.add(new FragmentIon(defaultMasses.get("E"),
			FragmentIonType.IMMONIUM_E, 0));
		break;
	    case 'Q':
		immoniumIons.add(new FragmentIon(defaultMasses.get("Q"),
			FragmentIonType.IMMONIUM_Q, 0));
		break;
	    case 'G':
		immoniumIons.add(new FragmentIon(defaultMasses.get("G"),
			FragmentIonType.IMMONIUM_G, 0));
		break;
	    case 'H':
		immoniumIons.add(new FragmentIon(defaultMasses.get("H"),
			FragmentIonType.IMMONIUM_H, 0));
		break;
	    case 'I':
		immoniumIons.add(new FragmentIon(defaultMasses.get("I"),
			FragmentIonType.IMMONIUM_I, 0));
		break;
	    case 'L':
		immoniumIons.add(new FragmentIon(defaultMasses.get("L"),
			FragmentIonType.IMMONIUM_L, 0));
		break;
	    case 'K':
		immoniumIons.add(new FragmentIon(defaultMasses.get("K"),
			FragmentIonType.IMMONIUM_K, 0));
		break;
	    case 'M':
		immoniumIons.add(new FragmentIon(defaultMasses.get("M"),
			FragmentIonType.IMMONIUM_M, 0));
		break;
	    case 'F':
		immoniumIons.add(new FragmentIon(defaultMasses.get("F"),
			FragmentIonType.IMMONIUM_F, 0));
		break;
	    case 'P':
		immoniumIons.add(new FragmentIon(defaultMasses.get("P"),
			FragmentIonType.IMMONIUM_P, 0));
		break;
	    case 'S':
		immoniumIons.add(new FragmentIon(defaultMasses.get("S"),
			FragmentIonType.IMMONIUM_S, 0));
		break;
	    case 'T':
		immoniumIons.add(new FragmentIon(defaultMasses.get("T"),
			FragmentIonType.IMMONIUM_T, 0));
		break;
	    case 'W':
		immoniumIons.add(new FragmentIon(defaultMasses.get("W"),
			FragmentIonType.IMMONIUM_W, 0));
		break;
	    case 'Y':
		immoniumIons.add(new FragmentIon(defaultMasses.get("Y"),
			FragmentIonType.IMMONIUM_Y, 0));
		break;
	    case 'V':
		immoniumIons.add(new FragmentIon(defaultMasses.get("V"),
			FragmentIonType.IMMONIUM_V, 0));
		break;
	    }
	}

	return immoniumIons.toArray(new FragmentIon[0]);
    }

    /**
     * Return the matched ions (MS/MS masses) between a calculated fragment ions
     * and scan's data points
     * 
     * @param scanDataPoints
     * @param fragmentIons
     * @param fragmentIonMassErrorTol
     * @param intensityThreshold
     * @return
     */
    public static DataPoint[] getMatchedIons(DataPoint[] scanDataPoints,
	    FragmentIon[] fragmentIons, double fragmentIonMassErrorTol,
	    double intensityThreshold) {
	DataPoint[] matchedDataPoints = null;

	return matchedDataPoints;
    }

    /**
     * Returns the coverage of the ion series.
     * 
     * @param dataPoints
     * @param peptide
     * @param ionType
     * @param intensityThreshold
     * @return
     */
    public static double getIonCoverage(DataPoint[] dataPoints,
	    Peptide peptide, SerieIonType ionType, double intensityThreshold) {

	PeptideFragmentation fragmentation = peptide.getFragmentation();
	FragmentIon[] fragmentIons = fragmentation.getFragmentIons(ionType);
	PeptideIdentityDataFile peptideDataFile = peptide.getScan()
		.getPeptideDataFile();
	double fragmentIonMassErrorTol = peptideDataFile
		.getFragmentIonMassErrorTolerance();

	DataPoint[] matchedDataPoints = getMatchedIons(dataPoints,
		fragmentIons, fragmentIonMassErrorTol, intensityThreshold);
	double ionCoverage = calculateSerieCoverage(matchedDataPoints,
		fragmentIons);

	return ionCoverage;
    }

    /**
     * Calculates the ion coverage
     * 
     * @param matchedDataPoints
     * @param fragmentIons
     * @return
     */
    public static double calculateSerieCoverage(DataPoint[] matchedDataPoints,
	    FragmentIon[] fragmentIons) {
	// TODO
	return 0;
    }

}
