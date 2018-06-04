package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.mymodule.isotopestuff.IsotopePatternCalculator2;

public class Tests {

	public static void main(String[] args) {
		System.out.println("Tests");
		//testIsotope();
		//test();
		isotopePatternTest();
	}
	
	private static void testIsotope() {
		//get isotope information, idk if it works
		Isotopes ifac;
		try {
			ifac = Isotopes.getInstance();
			IIsotope[] el = ifac.getIsotopes("Gd");
			el = (IIsotope[]) Arrays.stream(el).filter(i -> i.getNaturalAbundance()>0.1).toArray(IIsotope[]::new);
			int size = el.length;
			System.out.println(size);
			for(IIsotope i : el)
				System.out.println("mass "+ i.getExactMass() + "   abundance "+i.getNaturalAbundance());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void test()
	{
		ArrayList<Integer> list = new ArrayList<Integer>(3);
		
		for(int i = 0; i < 50; i++)
			list.add(i);
		System.out.println(list.size() + " last entry: " + list.get(list.size()-1));
	}
	
	private static void isotopePatternTest()
	{
		String formula = "ClBr";
		IsotopePattern pattern = IsotopePatternCalculator2.calculateIsotopePattern(formula, 0.01, 1, PolarityType.NEGATIVE);
		pattern = IsotopePatternCalculator2.mergeIsotopes(pattern, 0.000904);
		int size = pattern.getNumberOfDataPoints();
		System.out.println("size: " + size);
		DataPoint[] points = pattern.getDataPoints();
		//System.out.println(pattern.getDescription());

		for(int i = 0; i<points.length; i++)
		{
			System.out.println(formula + "Peak " + i + ": m/z: " + points[i].getMZ() + "\tI: " + points[i].getIntensity());
		}
		System.out.println(getIntensityRatios(pattern));
		System.out.println(IsotopePatternCalculator2.lastPattern.toString());
	}
	
	private static String getIntensityRatios(IsotopePattern pattern)
	{
		DataPoint[] dp = pattern.getDataPoints();
		String ratios = "";
		for(int i = 0; i < dp.length; i++)
			ratios += round((dp[i].getIntensity()/dp[0].getIntensity()), 2) + ":";
		ratios = (ratios.length() > 0) ? ratios.substring(0, ratios.length()-1) : ratios;
		return 	ratios;
	}
	public static double round(double value, int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
