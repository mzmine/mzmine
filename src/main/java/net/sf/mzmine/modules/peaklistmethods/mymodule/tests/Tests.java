package net.sf.mzmine.modules.peaklistmethods.mymodule.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.mymodule.isotopestuff.IsotopePatternCalculator2;

public class Tests {

	public static void main(String[] args) {
		
		exIPT();
		isotopePatternTest();
	
	}
		
	private static void isotopePatternTest()
	{
		String formula = "C12Cl6";
		IsotopePattern pattern = IsotopePatternCalculator.calculateIsotopePattern(formula, 0.1, 1, PolarityType.NEGATIVE);
		pattern = IsotopePatternCalculator.mergeIsotopes(pattern, 0.000904);
		int size = pattern.getNumberOfDataPoints();
		System.out.println("size: " + size);
		
		pattern = IsotopePatternCalculator.normalizeIsotopePattern(pattern, 1, 1.0);
		DataPoint[] points2 = pattern.getDataPoints();
		//System.out.println(pattern.getDescription());

		for(int i = 0; i<points2.length; i++)
		{
			System.out.println(formula + "Peak " + i + ": m/z: " + points2[i].getMZ() + "\tI: " + points2[i].getIntensity());
		}
		System.out.println(getIntensityRatios(pattern));
	}
	
	private static String getIntensityRatios(IsotopePattern pattern)
	{
		DataPoint[] dp = pattern.getDataPoints();
		String ratios = "";
		for(int i = 0; i < dp.length; i++)
			ratios += round((dp[i].getIntensity()/dp[0].getIntensity()),2) + ":";
		ratios = (ratios.length() > 0) ? ratios.substring(0, ratios.length()-1) : ratios;
		return 	ratios;
	}
	public static double round(double value, int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static void testMolFor()
	{
		String molecule = "C5Cl2H7";
		IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
		IMolecularFormula molFor = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(molecule, builder);
		IMolecularFormula newFor = MolecularFormulaManipulator.getMolecularFormula("C", builder);
		newFor.removeAllIsotopes();
		
		System.out.println(molFor.getIsotopeCount());
		List<IElement> elements = MolecularFormulaManipulator.elements(molFor);
		System.out.println(elements.toString());
		
		for(IIsotope isotopes : molFor.isotopes())
		{
			System.out.println(isotopes.getSymbol());
			newFor.addIsotope(isotopes, molFor.getIsotopeCount(isotopes)+1);
		}
		
		for(IIsotope isos : newFor.isotopes())
		{
			System.out.println(isos.getSymbol() + newFor.getIsotopeCount(isos));
		}
	}
	
	public static void exIPT()
	{
		ExtendedIsotopePattern p = new ExtendedIsotopePattern();
		//p.addElement("C5");
		p.setUpFromFormula("C12Cl6", 0.1);
		p.applyCharge(1, PolarityType.NEGATIVE);
		//p.addElement("Cl");
		
		DataPoint[] dps = p.getDataPoints();
		for(int i = 0; i < dps.length; i++)
		{
			System.out.println("mass: " + dps[i].getMZ() + "\tintensity: " + dps[i].getIntensity() + p.getExplicitPeakDescription(i));
		}
		System.out.println("-------------------------------------------------------\n"
						 + "-------------------------MERGE-------------------------\n"
						 + "-------------------------------------------------------");
		//p.mergePeaks(0.3);
		DataPoint[] dps2 = p.getDataPoints();
		for(int i = 0; i < dps2.length; i++)
		{
			System.out.println("mass: " + dps2[i].getMZ() + "\t\tintensity: " + dps2[i].getIntensity() + "\t" + p.getSimplePeakDescription(i)+ "\t---\t"+ p.getExplicitPeakDescription(i));
		}
	}
}
