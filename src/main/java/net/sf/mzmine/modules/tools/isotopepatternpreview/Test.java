package net.sf.mzmine.modules.tools.isotopepatternpreview;

import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.ExtendedIsotopePattern;

public class Test {
  public static void main(String[] args) {
    ExtendedIsotopePattern pattern = new ExtendedIsotopePattern();
    Logger logger = Logger.getLogger("Test");
    
    pattern.setUpFromFormula("C6Cl2", 0.01, 0.02, 0.03);
    pattern.applyCharge(1, PolarityType.POSITIVE);
    
    
    logger.info(pattern.getDetailedPeakDescription(2));
    logger.info("----------------");
    pattern.print();
  }
}
