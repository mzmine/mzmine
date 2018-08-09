package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

public class LipidIdentityChain extends SimplePeakIdentity {

  private final double mass;
  private final String sumFormula;

  public LipidIdentityChain(final LipidType lipidType, final int fattyAcid1Length,
      final int fattyAcid1DoubleBonds, final int oxidationValue) {

    this(lipidType.getAbbr() + '(' + fattyAcid1Length + ':' + fattyAcid1DoubleBonds + ')',
        lipidType.getFormula() + calculateFattyAcidFormula(fattyAcid1Length, fattyAcid1DoubleBonds),
        oxidationValue);
  }

  private LipidIdentityChain(final String name, final String formula, final int oxidationValue) {
    super(name);
    mass = FormulaUtils.calculateExactMass(formula);
    sumFormula = formula;
    if (oxidationValue == 0) {
      setPropertyValue(PROPERTY_NAME, name);
    }
    if (oxidationValue > 0) {
      setPropertyValue(PROPERTY_NAME, name + " + " + oxidationValue + "O");
    }
    setPropertyValue(PROPERTY_FORMULA, formula);
    setPropertyValue(PROPERTY_METHOD, "Lipid prediction");
  }

  /**
   * Calculate fatty acid formula.
   *
   * @param fattyAcidLength acid length.
   * @param fattyAcidDoubleBonds double bond count.
   * @return fatty acid formula.
   */
  private static String calculateFattyAcidFormula(final int fattyAcidLength,
      final int fattyAcidDoubleBonds) {

    String fattyAcid1Formula = "H";
    if (fattyAcidLength > 0) {

      final int numberOfHydrogens = fattyAcidLength * 2 - fattyAcidDoubleBonds * 2 - 1;
      fattyAcid1Formula = "C" + fattyAcidLength + 'H' + numberOfHydrogens;

    }
    return fattyAcid1Formula;
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
    return new LipidIdentityChain(getName(), getPropertyValue(PROPERTY_FORMULA), 0);
  }
}
