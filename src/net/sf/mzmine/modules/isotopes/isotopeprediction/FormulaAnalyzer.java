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

package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.identification.pubchem.TypeOfIonization;
import net.sf.mzmine.util.DataPointSorter;

import org.openscience.cdk.ChemObject;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.interfaces.IIsotope;

public class FormulaAnalyzer {

	private IsotopeFactory isoFactory;
	private DataPoint[] abundanceAndMass = null;
	private String errorMessage;
	private static double ELECTRON_MASS = 0.00054857d;
	// This value is the average from difference of masses between isotopes.
	private static double ISOTOPE_DISTANCE = 1.002d;

	/**
	 * This class generates an IsotopePattern using a chemical formula
	 * (empirical)
	 */
	public FormulaAnalyzer() {
		try {
			isoFactory = IsotopeFactory.getInstance(new ChemObject()
					.getBuilder());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method returns an IsotopePattern. The originalFormula must be an
	 * empirical chemical formula
	 * 
	 * @param originalFormula
	 * @param minAbundance
	 * @return
	 * @throws Exception
	 */
	public IsotopePattern getIsotopePattern(String originalFormula,
			double minAbundance, int charge, boolean positiveCharge,
			double isotopeHeight, boolean autoHeight, boolean sumOfMasses,
			TypeOfIonization ionization) throws Exception {

		int numOpenParenthesis = 0, numCloseParenthesis = 0;
		
		String mf = originalFormula.trim();
		charge = Math.abs(charge);
		mf = removeSpaces(mf);
		mf = removeSymbols(mf);

		if ((mf == null) || (mf == "")) {
			errorMessage = "Please type a chemical formula or common organic compound.";
			throw new Exception();
		}

		// Verify if the passed originalFormula is an abbreviation of a common
		// organic compound
		mf = getChemicalFormula(mf);

		// Analyze formula's syntaxes
		for (int i = 0; i < mf.length(); i++) {
			if (mf.charAt(i) == '(')
				numOpenParenthesis++;
			if (mf.charAt(i) == ')')
				numCloseParenthesis++;
		}

		if (numOpenParenthesis != numCloseParenthesis) {
			errorMessage = "Missing one or more parenthesis in the formula "
					+ mf;
			throw new Exception();
		}

		// In case of a formula with functional groups is necessary to unfold
		// the formula in order to get the exact number of atoms per element.
		mf = getUnfoldedFormula(mf);

		// Add coefficient for elements with just one atom
		boolean currentIsLetter, nextIsLetter, nextIsUpperCase;
		for (int i = 0; i < mf.length() - 1; i++) {
			currentIsLetter = Character.isLetter(mf.charAt(i));
			nextIsLetter = Character.isLetter(mf.charAt(i + 1));
			nextIsUpperCase = Character.isUpperCase(mf.charAt(i + 1));
			if (currentIsLetter && nextIsLetter) {
				if (nextIsUpperCase) {
					mf = mf.substring(0, i + 1) + "1" + mf.substring(i + 1);
					i = 0;
				}
			}
		}
		if (Character.isLetter(mf.charAt(mf.length() - 1))) {
			mf += "1";
		}

		// Divide the chemical formula into tokens (element and coefficients)
		HashMap<String, Integer> tokens;
		tokens = getFormulaInTokens(mf);
		
		// Load or remove ions depending of ionization type
		tokens = loadIonization(tokens, ionization);

		if (tokens == null) {
			errorMessage = "It is not possible to divide into tokens (elements and coefficients) the formula "
					+ mf + " .Please remove special characters";
			throw new Exception();
		}

		if (tokens.size() == 0) {
			errorMessage = "It is not possible to divide into tokens (elements and coefficients) the formula "
					+ mf + " . Please remove special characters";
			throw new Exception();
		}
		
		// Calculate abundance and mass
		Iterator<String> itr = tokens.keySet().iterator();
		String elementSymbol;

		int atomCount;
		while (itr.hasNext()) {
			elementSymbol = itr.next();
			atomCount = tokens.get(elementSymbol);

			for (int i = 0; i < atomCount; i++) {
				if (!calculateAbundanceAndMass(elementSymbol)) {
					errorMessage = "Chemical element not valid "
							+ elementSymbol;
					throw new Exception();
				}
			}
		}

		// Normalize the intensity of all isotopes in relation to the most
		// abundant isotope.
		abundanceAndMass = normalizeArray(abundanceAndMass, minAbundance);

		// Format isotope's mass according with charge distribution
		abundanceAndMass = loadChargeDistribution(abundanceAndMass, charge,
				positiveCharge);

		if (sumOfMasses)
			abundanceAndMass = createSingleIsotopePeaks(abundanceAndMass,
					charge);

		if (abundanceAndMass == null) {
			errorMessage = "It is not possible to process the formula " + mf;
			throw new Exception();
		}

		if (abundanceAndMass.length == 0) {
			errorMessage = "It is not possible to process the formula " + mf;
			throw new Exception();
		}

		int chargeDistribution = charge * (positiveCharge ? 1 : -1);

		// Get the formula (string) expressed according with C,H,O,N ...
		String finalFormula = getFinalFormula(tokens);

		// Form the IsotopePattern to be displayed.
		PredictedIsotopePattern isotopePattern = new PredictedIsotopePattern(
				abundanceAndMass, finalFormula, chargeDistribution);
		
		abundanceAndMass = null;

		if (!autoHeight)
			isotopePattern.setIsotopeHeight(isotopeHeight);
		else
			isotopePattern.setIsotopeHeight(0.0f);

		return isotopePattern;

	}

	/**
	 * Calculates the mass and abundance of all isotopes generated by adding one
	 * atom. Receives the periodic table element and calculate the isotopes, if
	 * there exist a previous calculation, add these new isotopes. In the
	 * process of adding the new isotopes, remove those that has an abundance
	 * less than setup parameter minAbundance, and remove duplicated masses.
	 * 
	 * @param elementSymbol
	 */
	private boolean calculateAbundanceAndMass(String elementSymbol) {

		IIsotope[] isotopes = isoFactory.getIsotopes(elementSymbol);

		if (isotopes == null)
			return false;

		if (isotopes.length == 0)
			return false;

		double mass, previousMass, abundance, totalAbundance, newAbundance;

		HashMap<Double, Double> isotopeMassAndAbundance = new HashMap<Double, Double>();
		TreeSet<DataPoint> dataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(true, true));

		DataPoint[] currentElementPattern = new DataPoint[isotopes.length];

		// Generate isotopes for the current atom (element)
		for (int i = 0; i < isotopes.length; i++) {
			mass = isotopes[i].getExactMass();
			abundance = isotopes[i].getNaturalAbundance();
			dataPoints.add(new SimpleDataPoint(mass, abundance));

		}

		currentElementPattern = dataPoints.toArray(new DataPoint[0]);
		dataPoints.clear();

		// Verify if there is a previous calculation. If it exists, add the new
		// isotopes
		if (abundanceAndMass == null) {

			abundanceAndMass = currentElementPattern;
			return true;

		} else {

			for (int i = 0; i < abundanceAndMass.length; i++) {

				totalAbundance = abundanceAndMass[i].getIntensity();

				if (totalAbundance == 0)
					continue;

				for (int j = 0; j < currentElementPattern.length; j++) {

					abundance = currentElementPattern[j].getIntensity();
					mass = abundanceAndMass[i].getMZ();

					if (abundance == 0)
						continue;

					newAbundance = totalAbundance * abundance * 0.01f;
					mass += currentElementPattern[j].getMZ();

					// Filter duplicated masses
					previousMass = searchMass(isotopeMassAndAbundance.keySet(),
							mass);
					if (isotopeMassAndAbundance.containsKey(previousMass)) {
						newAbundance += isotopeMassAndAbundance
								.get(previousMass);
						mass = previousMass;
					}

					// Filter isotopes too small
					if (isNotZero(newAbundance)) {
						isotopeMassAndAbundance.put(mass, newAbundance);
					}
					previousMass = 0;
				}
			}

			Iterator<Double> itr = isotopeMassAndAbundance.keySet().iterator();
			int i = 0;
			abundanceAndMass = new DataPoint[isotopeMassAndAbundance.size()];
			while (itr.hasNext()) {
				mass = itr.next();
				dataPoints.add(new SimpleDataPoint(mass,
						isotopeMassAndAbundance.get(mass)));
				i++;
			}
			abundanceAndMass = dataPoints.toArray(new DataPoint[0]);
		}

		return true;

	}

	public String getMessageError() {
		return errorMessage;
	}

	private static double searchMass(Set<Double> keySet, double mass) {
		double TOLERANCE = 0.00005f;
		double diff;
		for (double key : keySet) {
			diff = Math.abs(key - mass);
			if (diff < TOLERANCE)
				return key;
		}

		return 0.0d;
	}

	private static boolean isNotZero(double number) {
		double pow = (double) Math.pow(10, 6);
		int fraction = (int) (number * pow);

		if (fraction <= 0)
			return false;

		return true;
	}

	/**
	 * Returns a singles string that contains the groupName repeated by
	 * groupNumber.
	 * 
	 * @param groupName
	 * @param groupNumber
	 * @return
	 */
	private static String getMultiGroupFormula(String groupName, int groupNumber) {
		String temp = "";
		for (int i = 0; i < groupNumber; i++)
			temp += groupName;
		return temp;
	}

	/**
	 * Returns the chemical formula if the groupName is an abbreviation of a
	 * common organic compound See CommonOrganicCompound class.
	 * 
	 * @param groupName
	 * @return
	 */
	private static String isCommonCompound(String groupName) {
		for (CommonOrganicCompound comp : CommonOrganicCompound.values())
			if (comp.getName().equals(groupName))
				return comp.getFormula();
		return "";
	}

	/**
	 * Returns the chemical formula in terms of elements of periodic table and
	 * coefficients for each one.
	 * 
	 * @param synFormula
	 * @return
	 */
	private static String getChemicalFormula(String synFormula) {
		int BeginMid = 0, EndMid = 0, i = 0;
		String Element = "", ccName = "", fragmentFormula = "", coefficient = "", chemicalFormula;
		boolean isDigit = false;

		chemicalFormula = synFormula;

		for (i = 0; i < chemicalFormula.length() - 1; i++) {

			if (Character.isUpperCase(chemicalFormula.charAt(i))) {

				BeginMid = i;

				while (Character.isLowerCase(chemicalFormula.charAt(i + 1))) {
					i++;
					if (i >= chemicalFormula.length() - 1)
						break;
				}

				EndMid = i + 1;
				Element = chemicalFormula.substring(BeginMid, EndMid);

				if (!(i >= chemicalFormula.length() - 1)) {

					isDigit = false;
					while (Character.isDigit(chemicalFormula.charAt(i + 1))) {
						i++;
						isDigit = true;
						if (i >= chemicalFormula.length() - 1)
							break;
					}
					if (isDigit) {
						coefficient = chemicalFormula.substring(EndMid, i + 1);
					}
				}

				ccName = isCommonCompound(Element);

				if (ccName != "") {
					if (isDigit) {

						fragmentFormula = getMultiGroupFormula(ccName, Integer
								.parseInt(coefficient));
						chemicalFormula = chemicalFormula
								.substring(0, BeginMid)
								+ fragmentFormula
								+ chemicalFormula.substring(i + 1,
										chemicalFormula.length());
						i = i + fragmentFormula.length() - Element.length() - 1;
						isDigit = false;

					} else {

						fragmentFormula = ccName;
						chemicalFormula = chemicalFormula
								.substring(0, BeginMid)
								+ fragmentFormula
								+ chemicalFormula.substring(EndMid,
										chemicalFormula.length());
						i = i + fragmentFormula.length() - Element.length();

					}
				}
			}
		}
		return chemicalFormula;
	}

	/**
	 * Returns an unfolded formula, without functional groups. Example (CH3)2 ->
	 * CH3CH3
	 * 
	 * @param foldedFormula
	 * @return
	 */
	private static String getUnfoldedFormula(String foldedFormula) {
		int beginIndex, endIndex, numTimes = 0;
		String unfoldedFormula, fragmentFormula;

		unfoldedFormula = foldedFormula;

		// Analyze if there exist a coefficient for the complete molecule.
		if (Character.isDigit(unfoldedFormula.charAt(0)))
			unfoldedFormula = "." + unfoldedFormula;

		for (int i = 0; i < unfoldedFormula.length() - 1; i++)
			if (unfoldedFormula.charAt(i) == '(') {

				beginIndex = i;
				while (unfoldedFormula.charAt(i + 1) != ')')
					i++;
				endIndex = i;

				fragmentFormula = unfoldedFormula.substring(beginIndex + 1,
						endIndex + 1);
				unfoldedFormula = replaceWithSpaceAt(unfoldedFormula,
						beginIndex, endIndex + 1);

				if (Character.isLetter(unfoldedFormula.charAt(i + 2))
						|| i + 2 == unfoldedFormula.length()
						|| unfoldedFormula.charAt(i + 2) == '(') {
					numTimes = 1;
				} else if (Character.isDigit(unfoldedFormula.charAt(i + 2))) {

					beginIndex = i + 2;
					i++;
					while (Character.isDigit(unfoldedFormula.charAt(i + 1))) {
						i++;
						if (i + 1 == unfoldedFormula.length())
							break;
					}
					endIndex = i + 1;
					numTimes = Integer.parseInt(unfoldedFormula.substring(
							beginIndex, endIndex));
					unfoldedFormula = replaceWithSpaceAt(unfoldedFormula,
							beginIndex, endIndex);

				}

				for (int j = 0; j < numTimes; j++)
					unfoldedFormula += fragmentFormula;

				numTimes = 0;
				fragmentFormula = "";
			}

		unfoldedFormula = removeSpaces(unfoldedFormula);

		for (int i = 0; i < unfoldedFormula.length(); i++)
			if (unfoldedFormula.charAt(i) == '.') {

				unfoldedFormula = replaceWithSpaceAt(unfoldedFormula, i, i + 1);

				if (Character.isLetter(unfoldedFormula.charAt(i + 1)))
					numTimes = 1;
				else if (Character.isDigit(unfoldedFormula.charAt(i + 1))) {

					beginIndex = i;
					while (Character.isDigit(unfoldedFormula.charAt(i + 1)))
						i++;
					endIndex = i;
					numTimes = Integer.parseInt(unfoldedFormula.substring(
							beginIndex, endIndex + 1));
					unfoldedFormula = replaceWithSpaceAt(unfoldedFormula,
							beginIndex, endIndex + 1);

				}

				beginIndex = i + 1;

				while ((i + 1 != unfoldedFormula.length())
						&& (unfoldedFormula.charAt(i + 1) != '.')
						&& (unfoldedFormula.charAt(i + 1) != '('))
					i++;

				endIndex = i;

				fragmentFormula = unfoldedFormula.substring(beginIndex,
						endIndex + 1);

				unfoldedFormula = replaceWithSpaceAt(unfoldedFormula,
						beginIndex, endIndex + 1);

				for (int j = 1; j <= numTimes; j++)
					unfoldedFormula += fragmentFormula;
			}

		unfoldedFormula = removeSpaces(unfoldedFormula);

		return unfoldedFormula;
	}

	/**
	 * 
	 * @param original
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private static String replaceWithSpaceAt(String original, int startIndex,
			int endIndex) {

		String modified = original;

		for (int i = startIndex; i <= endIndex; i++) {
			if (i + 1 == modified.length()) {
				modified = modified.substring(0, i) + new Character(' ');
				break;
			} else {
				modified = modified.substring(0, i) + new Character(' ')
						+ modified.substring(i + 1);
			}

		}

		return modified;

	}

	/**
	 * Divide the formula into tokens (elements and coefficients)
	 * 
	 * @param mf
	 * @return
	 */
	private static HashMap<String, Integer> getFormulaInTokens(String mf) {

		Vector<String> element = new Vector<String>();
		Vector<Integer> coefficient = new Vector<Integer>();

		int beginIndex = 0, endIndex;
		int numberElements = 0;
		boolean nextIsDigit, currentIsDigit, currentIsLetter, nextIsLetter;

		for (int i = 0; i < mf.length() - 1; i++) {

			currentIsDigit = Character.isDigit(mf.charAt(i));
			currentIsLetter = Character.isLetter(mf.charAt(i));
			nextIsDigit = Character.isDigit(mf.charAt(i + 1));
			nextIsLetter = Character.isLetter(mf.charAt(i + 1));

			if (currentIsLetter && nextIsDigit) {

				endIndex = i + 1;
				element.add(mf.substring(beginIndex, endIndex));
				numberElements++;
				beginIndex = endIndex;
				continue;

			}
			if (currentIsDigit && nextIsLetter) {

				endIndex = i + 1;
				coefficient.add(Integer.parseInt(mf.substring(beginIndex,
						endIndex)));
				beginIndex = endIndex;
				continue;

			}
		}

		coefficient
				.add(Integer.parseInt(mf.substring(beginIndex, mf.length())));

		if (element.size() != coefficient.size())
			return null;

		HashMap<String, Integer> formulaInTokens = new HashMap<String, Integer>();
		int length = element.size();
		int value;

		for (int i = 0; i < length; i++) {

			if (formulaInTokens.containsKey(element.get(i))) {

				value = formulaInTokens.get(element.get(i))
						+ coefficient.get(i);
				formulaInTokens.put(element.get(i), value);

			} else
				formulaInTokens.put(element.get(i), coefficient.get(i));

		}

		return formulaInTokens;
	}

	/**
	 * Return a String that contains the empirical chemical formula, in terms of
	 * C,H,O,N,..
	 * 
	 * @param tokens
	 * @return
	 */
	private static String getFinalFormula(HashMap<String, Integer> tokens) {

		String formula = "";

		if (tokens.containsKey("C")) {
			formula += "C" + tokens.get("C");
		}

		if (tokens.containsKey("H")) {
			formula += "H" + tokens.get("H");
		}

		if (tokens.containsKey("N")) {
			formula += "N" + tokens.get("N");
		}

		if (tokens.containsKey("O")) {
			formula += "O" + tokens.get("O");
		}

		Iterator<String> itr = tokens.keySet().iterator();
		String elementSymbol;

		int atomCount;
		while (itr.hasNext()) {
			elementSymbol = itr.next();
			if ((elementSymbol.equals("C")) || (elementSymbol.equals("H"))
					|| (elementSymbol.equals("N"))
					|| (elementSymbol.equals("O"))) {

				continue;

			} else {

				atomCount = tokens.get(elementSymbol);
				formula += elementSymbol + atomCount;

			}

		}

		return formula;
	}

	/**
	 * Normalize the intensity (relative abundance) of all isotopes in relation
	 * of the most abundant isotope.
	 * 
	 * @param dataPoints
	 * @return
	 */
	private static DataPoint[] normalizeArray(DataPoint[] dataPoints,
			double minAbundance) {

		TreeSet<DataPoint> sortedDataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(true, true));

		double intensity, biggestIntensity = 0.0f;

		for (DataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			if (intensity > biggestIntensity)
				biggestIntensity = intensity;

		}

		for (DataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			intensity /= biggestIntensity;
			if (intensity < 0)
				intensity = 0;

			((SimpleDataPoint) dp).setIntensity(intensity);

		}

		for (DataPoint dp : dataPoints) {
			if (dp.getIntensity() >= (minAbundance ))
				sortedDataPoints.add(dp);
		}

		return sortedDataPoints.toArray(new DataPoint[0]);

	}

	/**
	 * 
	 * @param dataPoints
	 * @param charge
	 * @param positiveCharge
	 * @return
	 */
	private static DataPoint[] loadChargeDistribution(DataPoint[] dataPoints,
			int charge, boolean positiveCharge) {

		if (charge == 0) {
			return dataPoints;
		} else {
			int sign;
			double mass;

			if (positiveCharge)
				sign = -1;
			else
				sign = 1;

			for (DataPoint dp : dataPoints) {

				mass = (dp.getMZ() + (charge * sign * ELECTRON_MASS)) / charge;
				((SimpleDataPoint) dp).setMZ(mass);
			}
			return dataPoints;
		}
	}
	
	
	private static HashMap<String, Integer> loadIonization(HashMap<String, Integer> tokens, TypeOfIonization ionization){

		String ion = ionization.getElement();
		int quantity = ionization.getSign() * (-1);
		
		if (tokens.containsKey(ion)){
			int atomCount = tokens.get(ion);
			atomCount += quantity;
			tokens.put(ion, atomCount);
		}
		
		return tokens;

	}

	/**
	 * 
	 * @param dataPoints
	 * @param charge
	 * @param positiveCharge
	 * @return
	 */
	private static DataPoint[] createSingleIsotopePeaks(DataPoint[] dataPoints,
			int charge) {

		double distance;

		if (charge == 0) {
			distance = ISOTOPE_DISTANCE;
		} else {
			distance = ISOTOPE_DISTANCE / charge;
		}

		TreeSet<DataPoint> sortedDataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(true, true));

		for (DataPoint localDp : dataPoints) {
			sortedDataPoints.add(localDp);
		}

		dataPoints = sortedDataPoints.toArray(new DataPoint[0]);
		sortedDataPoints.clear();
		sortedDataPoints.add(dataPoints[0]);

		double localMass, nextIsotopeMass;
		nextIsotopeMass = dataPoints[0].getMZ() + distance;

		for (DataPoint localDp : dataPoints) {
			localMass = localDp.getMZ();
			if (localMass > nextIsotopeMass)
				sortedDataPoints
						.add(getGroupedDataPoint(localMass, dataPoints));
		}

		return sortedDataPoints.toArray(new DataPoint[0]);
	}

	/**
	 * Search and find the closest group of DataPoints in an array to the given
	 * mass. Always return a DataPoint
	 * 
	 * @param dp
	 * @param dataPoints
	 * @return DataPoint
	 */
	private static DataPoint getGroupedDataPoint(double mass,
			DataPoint[] dataPoints) {

		double diff, tolerance = ISOTOPE_DISTANCE / 4.0d;
		DataPoint dp;
		TreeSet<DataPoint> sortedDataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(false, false));

		for (DataPoint localDp : dataPoints) {
			diff = Math.abs(mass - localDp.getMZ());
			if (diff <= tolerance) {
				sortedDataPoints.add(localDp);
			}
		}

		double averageWeightMass = 0, totalIntensity = 0;
		Iterator<DataPoint> itr = sortedDataPoints.iterator();
		while (itr.hasNext()) {
			dp = itr.next();
			averageWeightMass += dp.getMZ() * dp.getIntensity();
			// totalMass += dp.getMZ();
			totalIntensity += dp.getIntensity();
		}

		averageWeightMass /= totalIntensity;

		return new SimpleDataPoint(averageWeightMass, totalIntensity);

	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	private static String removeSpaces(String s) {
		StringTokenizer st = new StringTokenizer(s, " ", false);
		String t = "";
		while (st.hasMoreElements())
			t += st.nextElement();
		return t;
	}
	
	private static String removeSymbols(String s){
		
		for (int i=0; i< s.length(); i++){
			if ((s.charAt(i) == '+') || (s.charAt(i) == '-')){
				s = replaceWithSpaceAt(s, i, i);
			}
		}
		
		s = removeSpaces(s);
		return s;
		
	}

}
