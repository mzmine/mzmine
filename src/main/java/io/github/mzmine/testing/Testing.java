package io.github.mzmine.testing;

import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.DefaultChemObjectBuilder;


public class Testing
{



	public static void main(String[] args) throws Exception
	{
		System.out.println("testing");

		// String[] testvals = new String[]{"test", "H2O9"};
		// String[] testvals = new String[]{"test", "H2O9", "H+", "(2H)+"};
		// String[] testvals = new String[]{"test", "blabla", "H2O9", "H", "H+", "(2H)+", "H-", "(H2)5O4H",
		// 								 "[H+]+", "[H2+2]+2", "[H2+2]2+", "[NH3-]-", "Cl"};
		String[] testvals = new String[]{"H", "H+", "H-", "[H+]+", "[H-]-", "C23H39N7O17P3S+",
										 "C10H16N5O10P2+", "[C20H34N6O12S2]2+", "[C21H30N7O17P3]2+"};

		for(String val: testvals){
			System.out.println();
			System.out.println(val);

			IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
			IMolecularFormula mf = MolecularFormulaManipulator.getMolecularFormula(val, builder);

			System.out.println("mf " + mf);
			System.out.println("mf get charge " + mf.getCharge());
			System.out.println("manip get mass " + MolecularFormulaManipulator.getMass(mf));
			System.out.println("manip get mass abund " + MolecularFormulaManipulator.getMass(mf, MolecularFormulaManipulator.MonoIsotopic));
			System.out.println("manip get string " + MolecularFormulaManipulator.getString(mf));

			for(IIsotope isotope: mf.isotopes()){
				System.out.println("iso " + isotope);
				System.out.println("iso get symbol " + isotope.getSymbol());
				System.out.println("iso get exact mass " + isotope.getExactMass());
				System.out.println("iso get mass number " + isotope.getMassNumber());
				System.out.println("iso get nat abund " + isotope.getNaturalAbundance());
				System.out.println("iso get atomic numer " + isotope.getAtomicNumber());

			}

			// System.out.println();

			// int charge = mf.getCharge();
			// charge = charge == null : 1 ? charge;
			// int charge = 0;
			int charge = 1;
			if(mf.getCharge() != null){
				charge = mf.getCharge();
			}

			double electronRestMass = 0.0005485799090;
			double mass = MolecularFormulaManipulator.getMass(mf, MolecularFormulaManipulator.MonoIsotopic);
			mass -= charge * electronRestMass;

			System.out.println("monoisotopic ionic mass " + mass);

			double mz = Math.abs(mass / charge);
			System.out.println("monoisotopic mz " + mz);

		}



	}

}