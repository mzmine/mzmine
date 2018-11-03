package net.sf.mzmine.modules.tools.isotopepatternpreview;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.FormulaUtils;

public class test {

  public static void main(String[] args) throws InterruptedException {
    System.out.print("Formula\tt/ms\tsize\tpattern time\n");
    
    String elements[] = {"C", "Cl", "Br"};
    for(int i = 1; i < 100; i+=10) {
      String formula = "";
      for(String s : elements) {
        formula += s + i;
      }
      long a = System.nanoTime();
      a = System.nanoTime();
      long size = FormulaUtils.getFormulaSize(formula);
      long b = System.nanoTime();
      System.out.print(formula + "\t" + ((b-a)/1000000) + "\t" + size);
      System.out.println("");
    }
  }
}
