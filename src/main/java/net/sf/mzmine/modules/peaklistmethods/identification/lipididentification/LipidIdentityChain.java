package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids2.LipidClasses;
import net.sf.mzmine.util.FormulaUtils;

public class LipidIdentityChain extends SimplePeakIdentity {

  private final double mass;
  private final String sumFormula;

  public LipidIdentityChain(final LipidClasses lipidClass, final int chainLength,
      final int chainDoubleBonds, final int numberOfAcylChains, final int numberOfAlkylChains) {

    this(lipidClass.getAbbr() + '(' + chainLength + ':' + chainDoubleBonds + ')',
        lipidClass.getBackBoneFormula() + calculateTotalChainFormula(chainLength, chainDoubleBonds,
            numberOfAcylChains, numberOfAlkylChains));
  }

  private LipidIdentityChain(final String name, final String formula) {
    super(name);
    mass = FormulaUtils.calculateExactMass(formula);
    sumFormula = formula;
    setPropertyValue(PROPERTY_NAME, name);
    setPropertyValue(PROPERTY_FORMULA, formula);
    setPropertyValue(PROPERTY_METHOD, "Lipid identification");
  }

  /**
   * Calculate fatty acid formula.
   *
   * @param chainLength acid length.
   * @param chainDoubleBonds double bond count.
   * @return fatty acid formula.
   */
  private static String calculateTotalChainFormula(final int chainLength,
      final int chainDoubleBonds, final int numberOfAcylChains, final int numberOfAlkylChains) {

    String chainFormula = "H";
    if (chainLength > 0) {

      final int numberOfHydrogens = chainLength * 2 - chainDoubleBonds * 2 - 1;
      chainFormula = "C" + chainLength + 'H' + numberOfHydrogens;

    }
    return chainFormula;
  }

  /**
   * Get the mass.
   *
   * @return the mass.
   */
  public double getMass() {
    return mass;
  }

  /**
   * Get the formula.
   *
   * @return the formula.
   */
  public String getFormula() {
    return sumFormula;
  }

  @Override
  public @Nonnull Object clone() {
    return new LipidIdentityChain(getName(), getPropertyValue(PROPERTY_FORMULA));
  }
}
