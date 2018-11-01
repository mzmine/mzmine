package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner;

import org.jmol.util.Logger;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePattern;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.FormulaUtils;

public class Test {
  
  
  public static void main(String[] args) {
    String s = "H    He    Li    Be    B    C    N    O    F    Ne    Na    Mg    Al    Si    P    S    Cl    Ar    K    Ca    Sc    Ti    V    Cr    Mn    Fe    Co    Ni    Cu    Zn    Ga    Ge    As    Se    Br    Kr    Rb    Sr    Y    Zr"
    + "    Nb    Mo    Tc    Ru    Rh    Pd    Ag    Cd    In    Sn    Sb    Te    I    Xe    Cs    Ba    La    Ce    Pr    Nd    Pm    Sm    Eu    Gd    Tb    Dy    Ho    Er    Tm    Yb    Lu    Hf    Ta    W    Re    Os    Ir    Pt"
    + "    Au    Hg    Tl    Pb    Bi    Po    At    Rn    Fr    Ra    Ac    Th    Pa    U    Np    Pu    Am    Cm    Bk    Cf    Es    Fm    Md    No    Lr    Rf    Db    Sg    Bh    Hs    Mt    Ds    Rg    Cn    Nh    Fl    Mc    Lv    Ts"
    + "    Og";
    String a = s.replace("    ", "1");
    System.out.println(a);
    FormulaUtils.checkMolecularFormula(a + "XR");
  }
  
  static boolean checkMolecularFormula(String formula) {
    
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula molFormula;
    
    molFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formula, builder);
    
    String simple = MolecularFormulaManipulator.simplifyMolecularFormula(formula);
    String elements[] = simple.split("0");
    
    String invalid = "";
    
    boolean found = false;
    for(String element : elements) {
      found = false;
      for(IIsotope iso : molFormula.isotopes()) {
        if(element.equals(iso.getSymbol()) && (iso.getMassNumber() != null)) {
          found = true;
        }
      }
      if(found == false) {
        invalid += element + ", ";
      }
    }
    
    if(invalid.length() != 0) {
      invalid = invalid.substring(0, invalid.length()-2);
      System.out.println("formula invalid! Element(s) " + invalid + " do not exist.");
      return false;
    }
    return true;
  }

  /*public static void main(String[] args) {
    IsotopePattern isos;
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula formula =
        MolecularFormulaManipulator.getMajorIsotopeMolecularFormula("C25Cl2", builder);
    IsotopePatternGenerator gen = new IsotopePatternGenerator(0.01);
    gen.setStoreFormulas(true);
    System.out.print(
        "--------------------------------------------------------\nCDK pure: C25Cl2 - minIntensity=0.01 - merge=default\n");
    isos = gen.getIsotopes(formula);
    for (IsotopeContainer container : isos.getIsotopes()) {
      System.out.print("mass=" + container.getMass() + "\tintensity=" + container.getIntensity()
          + "\tformula=" + container.toString() + "\n");
    }

    System.out.print(
        "--------------------------------------------------------\nCDK merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    gen.setMinResolution(0.01);
    isos = gen.getIsotopes(formula);
    for (IsotopeContainer container : isos.getIsotopes()) {
      System.out.print("mass=" + container.getMass() + "\tintensity=" + container.getIntensity()
          + "\tformula=" + container.toString() + "\n");
    }


    net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern pat2 = (net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern) IsotopePatternCalculator
        .calculateIsotopePattern("C25Cl2", 0.01, 0.01, 0, PolarityType.NEUTRAL, true);

    DataPoint[] dp = pat2.getDataPoints();
    System.out.print(
        "--------------------------------------------------------\nMZmineEx merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    for (int i = 0; i < pat2.getNumberOfDataPoints(); i++) {

      System.out.print("mass=" + dp[i].getMZ() + "\tintensity=" + dp[i].getIntensity() + "\tMF="
          + pat2.getIsotopeComposition(i) + "\n");
    }
    net.sf.mzmine.datamodel.impl.SimpleIsotopePattern pat =
        (net.sf.mzmine.datamodel.impl.SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern("C25Cl2", 0.01, 0.01, 0, PolarityType.NEUTRAL, false);
    dp = pat.getDataPoints();
    System.out.print(
        "--------------------------------------------------------\nMZmineSim merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    for (int i = 0; i < pat.getNumberOfDataPoints(); i++) {

      System.out.print("mass=" + dp[i].getMZ() + "\tintensity=" + dp[i].getIntensity() + "\n");
    }
    
    System.out.print(
        "--------------------------------------------------------\nSteffen merged: C25Cl2 - minIntensity=0.01 - merge=0.01\n");
    ExtendedIsotopePattern exip = new ExtendedIsotopePattern();
    exip.setUpFromFormula("C25Cl2", 0.01, 0.01, 0.01);
    exip.print();
  }*/
}
