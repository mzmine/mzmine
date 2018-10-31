package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner;

import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePattern;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;

public class Test {

  public static void main(String[] args) {
    IsotopePattern isos;
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula formula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula("C25Cl2", builder);
    IsotopePatternGenerator gen = new IsotopePatternGenerator(0.01);
    gen.setStoreFormulas(true);
    System.out.print("--------------------------------------------------------\nCDK pure: C25Cl2 - minIntensity=0.01 - merge=default\n");
    isos = gen.getIsotopes(formula);
    for (IsotopeContainer container : isos.getIsotopes()) {
      System.out.print("mass=" + container.getMass() +  "\tintensity=" +  container.getIntensity() + "\tformula=" +  container.toString() + "\n");
    }
    
    System.out.print("--------------------------------------------------------\nCDK merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    gen.setMinResolution(0.01);
    isos = gen.getIsotopes(formula);
    for (IsotopeContainer container : isos.getIsotopes()) {
      System.out.print("mass=" + container.getMass() +  "\tintensity=" +  container.getIntensity() + "\tformula=" +  container.toString() + "\n");
    }
    
    
    SimpleIsotopePattern pat2 = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern("C25Cl2", 0.01, 0, PolarityType.NEUTRAL, true);
    
    DataPoint[] dp = pat2.getDataPoints();
    System.out.print("--------------------------------------------------------\nMZmine pure: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    for(int i = 0; i < pat2.getNumberOfDataPoints(); i++) {
      
      System.out.print("mass=" + dp[i].getMZ() + "\tintensity=" + dp[i].getIntensity() + "\tMF=" + pat2.getIsotopeComposition(i) + "\n");
    }
    SimpleIsotopePattern pat = (SimpleIsotopePattern) IsotopePatternCalculator.mergeIsotopes(pat2, 0.01);
    dp = pat.getDataPoints();
    System.out.print("--------------------------------------------------------\nMZmine merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    for(int i = 0; i < pat.getNumberOfDataPoints(); i++) {
      
      System.out.print("mass=" + dp[i].getMZ() + "\tintensity=" + dp[i].getIntensity() + "\tMF=" + pat.getIsotopeComposition(i) + "\n");
    }
  }
}
