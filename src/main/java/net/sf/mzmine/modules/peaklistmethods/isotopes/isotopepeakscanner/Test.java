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

public class Test {

  public static void main(String[] args) {
    IsotopePattern isos;
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula formula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula("C13Cl2", builder);
    IsotopePatternGenerator gen = new IsotopePatternGenerator(0.01);
    
    isos = gen.getIsotopes(formula);
    for (IsotopeContainer container : isos.getIsotopes()) {
      System.out.print("mass=" + container.getMass() +  "\tintensity=" +  container.getIntensity() + "\tformula=" +  container.getFormulasString() + "\n");
    }
  }
}
