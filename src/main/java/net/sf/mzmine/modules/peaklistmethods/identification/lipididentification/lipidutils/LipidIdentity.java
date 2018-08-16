package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils;

import java.util.Map;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.util.FormulaUtils;

public class LipidIdentity extends SimplePeakIdentity {

  private double exactMass;
  private String sumFormula;
  private static LipidChainBuilder chainBuilder = new LipidChainBuilder();

  public LipidIdentity(final LipidClasses lipidClass, final int chainLength,
      final int chainDoubleBonds, final int numberOfAcylChains, final int numberOfAlkylChains) {

    this(
        lipidClass.getName() + " " + lipidClass.getAbbr() + '(' + chainLength + ':'
            + chainDoubleBonds + ')',
        lipidClass.getBackBoneFormula() + chainBuilder.calculateChainFormula(chainLength,
            chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains));

  }

  private LipidIdentity(final String name, final String formula) {
    super(name);
    // Parse formula
    Map<String, Integer> parsedFormula = FormulaUtils.parseFormula(formula);
    // Rearrange formula
    sumFormula = FormulaUtils.formatFormula(parsedFormula);
    exactMass = FormulaUtils.calculateExactMass(sumFormula);
    setPropertyValue(PROPERTY_NAME, name);
    setPropertyValue(PROPERTY_FORMULA, sumFormula);
    setPropertyValue(PROPERTY_METHOD, "Lipid identification");
  }

  public double getMass() {
    return exactMass;
  }

  public String getFormula() {
    return sumFormula;
  }

  @Override
  public @Nonnull Object clone() {
    return new LipidIdentity(getName(), getPropertyValue(PROPERTY_FORMULA));
  }
}
