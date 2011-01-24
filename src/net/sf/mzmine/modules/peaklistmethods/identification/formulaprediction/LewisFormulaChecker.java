package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LewisFormulaChecker {

	public static final Map<String, Integer> valences = new HashMap<String, Integer>();
	/**
	 * This defines the typical valence states, in fact e.g. sulphur may have valence 2, 4 or 6 
	 */
	static {
		valences.put("H", 1);
		valences.put("B", 3);
		valences.put("C", 4);
		valences.put("N", 3);
		valences.put("O", 2);
		valences.put("F", 1);
		valences.put("Na", 1);
		valences.put("Mg", 2);
		valences.put("Al", 3);
		valences.put("Si", 4);
		valences.put("P", 3);
		valences.put("S", 2);
		valences.put("Cl", 1);
		valences.put("Br", 1);
		valences.put("Ca", 2);
		valences.put("I", 1);
	}
	
	public static final Map<String, Integer> maxValences = new HashMap<String, Integer>();
	/**
	 * This defines the typical valence states, in fact e.g. sulphur may have valence 2, 4 or 6 
	 */
	static {
		maxValences.put("H", 1);
		maxValences.put("B", 3);
		maxValences.put("C", 4);
		maxValences.put("N", 5);
		maxValences.put("O", 2);
		maxValences.put("F", 1);
		maxValences.put("Na", 1);
		maxValences.put("Mg", 2);
		maxValences.put("Al", 3);
		maxValences.put("Si", 4);
		maxValences.put("P", 5);
		maxValences.put("S", 6);
		maxValences.put("Cl", 1);
		maxValences.put("Br", 1);
		maxValences.put("Ca", 2);
		maxValences.put("I", 1);
	}

	/**
	 * Note: RDBE values may get as low as -4 (CH3F10NS2)
	 *
	 */
	public static double calculateRDBE(IMolecularFormula formula) {

		double sum = 2;

		for (IIsotope isotope : formula.isotopes()) {

			Integer valence = valences.get(isotope.getSymbol());
			if (valence == null)
				continue;
			sum += (valence - 2) * formula.getIsotopeCount(isotope);
		}
		return (sum / 2);
	}
	
	public static int calculateE(IMolecularFormula formula) {

		int sum = 0;
		HashMap<String,Integer> valencesTable = new HashMap<String,Integer>();
        valencesTable.put("H", 1);
        valencesTable.put("Li", 1);
        valencesTable.put("Be", 2);
        valencesTable.put("B", 3);
        valencesTable.put("C", 4);
        valencesTable.put("N", 5);
        valencesTable.put("O", 6);
        valencesTable.put("F", 7);
        valencesTable.put("Na", 1);
        valencesTable.put("Mg", 2);
        valencesTable.put("Al", 3);
        valencesTable.put("Si", 4);
        valencesTable.put("P", 5);
        valencesTable.put("S", 6);
        valencesTable.put("Cl", 7);
        valencesTable.put("K", 1);
        valencesTable.put("Ca", 2);
        valencesTable.put("Ga", 3);
        valencesTable.put("Ge", 4);
        valencesTable.put("As", 5);
        valencesTable.put("Se", 6);
        valencesTable.put("Br", 7);
        valencesTable.put("Rb", 1);
        valencesTable.put("Sr", 2);
        valencesTable.put("In", 3);
        valencesTable.put("Sn", 4);
        valencesTable.put("Sb", 5);
        valencesTable.put("Te", 6);
        valencesTable.put("I", 7);
        valencesTable.put("Cs", 1);
        valencesTable.put("Ba", 2);
        valencesTable.put("Tl", 3);
        valencesTable.put("Pb", 4);
        valencesTable.put("Bi", 5);
        valencesTable.put("Po", 6);
        valencesTable.put("At", 7);
        valencesTable.put("Fr", 1);
        valencesTable.put("Ra", 2);
        valencesTable.put("Cu", 2);
        valencesTable.put("Mn", 2);
        valencesTable.put("Co", 2);

        
		for (IIsotope isotope : formula.isotopes()) {
			
			Integer maxOxidationNumber =  valencesTable.get(isotope.getSymbol());
			if (maxOxidationNumber == null)
				continue;
			sum += (maxOxidationNumber) * formula.getIsotopeCount(isotope);
		}
		return sum;
	}
	
	public static int calculateLewisSum(IMolecularFormula formula) {

		int sum = 0;
        
		for (IIsotope isotope : formula.isotopes()) {
			
			Integer valence =  valences.get(isotope.getSymbol());
			if (valence == null)
				continue;
			sum += (valence) * formula.getIsotopeCount(isotope);
		}
		return sum;
	}
	


	public static boolean checkLewisOctetRule(IMolecularFormula formula) {

		int sume = calculateE(formula);
		int ls = calculateLewisSum(formula);
		return (sume > 7) && (ls % 2 == 0);
		
	}

	public static boolean checkSeniorRule(IMolecularFormula formula) {
		
		double sume = calculateE(formula);
		double atoms = getAtoms(formula);
		
		return sume >= (2 * (atoms  -1));
	}
	
	public static int getAtoms(IMolecularFormula formula) {

		int sum = 0;
        
		for (IIsotope isotope : formula.isotopes()) {
			sum += formula.getIsotopeCount(isotope);
		}
		return sum;
	}
	
	public static boolean getHC(IMolecularFormula formula) {

		double carb = 0;
		double hyd = 0;
		for (IIsotope isotope : formula.isotopes()) {
			if (isotope.getSymbol().equals("C")) carb += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("H")) hyd += formula.getIsotopeCount(isotope);
		}

		if (carb == 0) return false;
		
		double rat = hyd / carb;
		return (rat > 0) && (rat < 6);
	}
	
	public static boolean getNOPS(IMolecularFormula formula) {

		double eC = 0, eH=0, eN=0, eO=0, eP=0, eS =0;
		for (IIsotope isotope : formula.isotopes()) {
			if (isotope.getSymbol().equals("C")) eC += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("H")) eH += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("N")) eN += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("O")) eO += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("P")) eP += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("S")) eS += formula.getIsotopeCount(isotope);
		}

		if (eC == 0) return false;
		
		double rPC = eP / eC;
		double rNC = eN / eC;
		double rOC = eO / eC;
		double rSC = eS / eC;

		return (rNC <= 4) && (rOC <= 3) && (rPC <=2) && (rSC <= 3);
	}

	public static boolean getHNOPS(IMolecularFormula formula) {

		double eC = 0, eH=0, eN=0, eO=0, eP=0, eS =0;
		for (IIsotope isotope : formula.isotopes()) {
			if (isotope.getSymbol().equals("C")) eC += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("H")) eH += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("N")) eN += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("O")) eO += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("P")) eP += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("S")) eS += formula.getIsotopeCount(isotope);
		}

		if (eC == 0) return false;
		
		double rHC = eH / eC;
		double rPC = eP / eC;
		double rNC = eN / eC;
		double rOC = eO / eC;
		double rSC = eS / eC;

		return (rHC >= 0.2) && (rHC <= 3) && (rNC <= 2) && (rOC <= 1.2) && (rPC <=0.32) && (rSC <= 0.65);
	}

	public static boolean checkExists(String formula) {
		IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
		IMolecularFormula f = MolecularFormulaManipulator.getMolecularFormula(
				formula, builder);
		
		return checkLewisOctetRule(f) && checkSeniorRule(f) && getHC(f) && getNOPS(f) && getHNOPS(f);
		
	}
	
	public static void main(String args[]) {

		IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
		IMolecularFormula f = MolecularFormulaManipulator.getMolecularFormula(
				"C6H12NO2", builder);
		String fs = MolecularFormulaManipulator.getString(f);

		int atoms = getAtoms(f);
		double rdb = calculateRDBE(f);
		double sume = calculateE(f);
		double ls = calculateLewisSum(f);
		boolean lewis = checkLewisOctetRule(f);
		boolean senior = checkSeniorRule(f);
		boolean hc = getHC(f);
		boolean nops = getNOPS(f);
		boolean hnops = getHNOPS(f);
		
		System.out.println(fs + " --> RDBE:" + rdb + " sumE:" + sume + " lewis_sum:" + ls + " LEWIS:" + lewis + " atoms:" + atoms  + " senior:" + senior);
		System.out.println(fs + " --> HC:" + hc+ " NOPS:" + nops + " HNOPS:" + hnops);

	}
}
