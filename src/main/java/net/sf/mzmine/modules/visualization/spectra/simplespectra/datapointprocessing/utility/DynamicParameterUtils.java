package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.utility;

import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.IsotopePatternUtils2;

public class DynamicParameterUtils {

  private static Logger logger = Logger.getLogger(DynamicParameterUtils.class.getName());

  private static float lowerElementBoundaryPercentage = 0.75f;
  private static float upperElementBoundaryPercentage = 1.5f;

  public static float getLowerElementBoundaryPercentage() {
    return lowerElementBoundaryPercentage;
  }

  public static float getUpperElementBoundaryPercentage() {
    return upperElementBoundaryPercentage;
  }

  public static void setLowerElementBoundaryPercentage(float lowerElementBoundaryPercentage) {
    DynamicParameterUtils.lowerElementBoundaryPercentage = lowerElementBoundaryPercentage;
  }

  public static void setUpperElementBoundaryPercentage(float upperElementBoundaryPercentage) {
    DynamicParameterUtils.upperElementBoundaryPercentage = upperElementBoundaryPercentage;
  }

  /**
   * Creates an ElementParameter based on the previous processing results. If no results were
   * detected, the default value is returned. Upper and lower boundaries are chosen according to
   * lowerElementBoundaryPercentage and upperElementBoundaryPercentage values of this utility class.
   * These values can be set via {@link #setLowerElementBoundaryPercentage} and
   * {@link #setUpperElementBoundaryPercentage}. The elements contained in
   * 
   * @param dp The data point to build a parameter for.
   * @param def The default set of parameters.
   * @return The built ElementsParameter
   */
  public static MolecularFormulaRange buildFormulaRangeOnIsotopePatternResults(
      ProcessedDataPoint dp, MolecularFormulaRange def) {

    DPPIsotopePatternResult result =
        (DPPIsotopePatternResult) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN);
    if (result == null)
      return def;

    if(!(result.getValue() instanceof ExtendedIsotopePattern))
      return def;
    
    ExtendedIsotopePattern pattern = (ExtendedIsotopePattern) result.getValue();
    String form = IsotopePatternUtils2.makePatternSuggestion(pattern.getIsotopeCompositions());
    
    MolecularFormulaRange range = new MolecularFormulaRange();

    IMolecularFormula formula =
        FormulaUtils.createMajorIsotopeMolFormula(form);
    if(formula == null) {
      logger.finest("could not generate formula for m/z " + dp.getMZ() + " " + form);
      return def;
    }

    for (IIsotope isotope : def.isotopes())
      range.addIsotope(isotope, def.getIsotopeCountMin(isotope),
          def.getIsotopeCountMax(isotope));

    for (IIsotope isotope : formula.isotopes()) {
      if (range.contains(isotope))
        continue;

      int count = formula.getIsotopeCount(isotope);

      range.addIsotope(isotope, (int) (count * lowerElementBoundaryPercentage),
          (int) (count * upperElementBoundaryPercentage));
    }

    for(IIsotope isotope : range.isotopes()) {
      int min = range.getIsotopeCountMin(isotope);
      int max = range.getIsotopeCountMax(isotope);
//      logger.info("m/z = " + dp.getMZ() + " " + isotope.getSymbol() + " " + min + " - " + max);
    }

    return range;
  }



}
