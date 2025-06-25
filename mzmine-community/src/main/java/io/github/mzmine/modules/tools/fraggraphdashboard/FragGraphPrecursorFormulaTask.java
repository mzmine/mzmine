/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.fraggraphdashboard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.FragmentUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class FragGraphPrecursorFormulaTask extends FxUpdateTask<FragDashboardModel> {

  private static final int DEFAULT_MAX_FORMULA_COUNT = 100;

  private final IonType ionTypeOverride;
  private final MZTolerance formulaTolerance;
  private final boolean checkNOPS;
  private final boolean checkCH;
  private final boolean checkMultiple;
  private final boolean checkRDBE;
  private final int maxFormulaCount;
  private final Integer charge;
  private final PolarityType polarity;
  private final double averageMZ;
  private final String desc;
  private final List<IonModification> assignedIonTypes;
  private final MassSpectrum ms2Spectrum;
  private final MassSpectrum measuredIsotopePattern;
  private final MolecularFormulaRange elements;
  private MolecularFormulaGenerator generator;
  private int formulaCount = 0;
  @NotNull
  private MZTolerance ms2Tolerance;

  public FragGraphPrecursorFormulaTask(@NotNull FragDashboardModel model, ParameterSet parameters) {
    this(model, null, //
        parameters.getValue(FragmentGraphCalcParameters.ms1Tolerance), //
        parameters.getEmbeddedParameterValue(FragmentGraphCalcParameters.heuristicParams)
            .getValue(ElementalHeuristicParameters.checkNOPS), //
        parameters.getEmbeddedParameterValue(FragmentGraphCalcParameters.heuristicParams)
            .getValue(ElementalHeuristicParameters.checkHC),
        parameters.getEmbeddedParameterValue(FragmentGraphCalcParameters.heuristicParams)
            .getValue(ElementalHeuristicParameters.checkMultiple), true, //
        parameters.getValue(FragmentGraphCalcParameters.maximumFormulae), //
        parameters.getValue(FragmentGraphCalcParameters.polarity), //
        parameters.getValue(FragmentGraphCalcParameters.adducts), //
        parameters.getValue(FragmentGraphCalcParameters.ms2Tolerance), //
        parameters.getValue(FragmentGraphCalcParameters.elements));
  }

  // todo make use of polarity and the assigned ion types if we have a row
  public FragGraphPrecursorFormulaTask(@NotNull FragDashboardModel model,
      @Nullable IonType ionTypeOverride, @NotNull MZTolerance formulaTolerance, boolean checkNOPS,
      boolean checkCH, boolean checkMultiple, boolean checkRDBE, int maxFormulaCount,
      @NotNull PolarityType polarity, @NotNull List<IonModification> assignedIonTypes,
      @NotNull MZTolerance ms2Tolerance, @NotNull MolecularFormulaRange elements) {
    super("Calculate precursor formulae", model);
    this.ionTypeOverride = ionTypeOverride;
    this.formulaTolerance = formulaTolerance;
    this.checkNOPS = checkNOPS;
    this.checkCH = checkCH;
    this.checkMultiple = checkMultiple;
    this.checkRDBE = checkRDBE;
    this.maxFormulaCount = maxFormulaCount;
    this.ms2Tolerance = ms2Tolerance;
    try {
      this.elements = (MolecularFormulaRange) elements.clone();
      assignedIonTypes.forEach(
          ion -> FragmentUtils.reflectIonTypeInFormulaRange(new IonType(ion), this.elements));
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    this.assignedIonTypes = assignedIonTypes;
    this.charge = 1;
    this.polarity = polarity;
    this.averageMZ = model.getPrecursorMz();
    this.desc = getName();
    this.ms2Spectrum = model.getSpectrum();
    this.measuredIsotopePattern = model.getIsotopePattern();
  }

  @Override
  public @NotNull String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return generator == null ? 0 : generator.getFinishedPercentage();
  }

  @Override
  public void process() {
    final boolean couldBeRadical = assignedIonTypes.stream().anyMatch(IonModification::isElectron);

    generator = setUpFormulaGenerator();
    generateFormulae(couldBeRadical, generator);
    if (formulaCount == 0) {
      DesktopService.getDesktop().displayMessage(
          "No formulae found. Consider changing the tolerances or the element ranges.");
    }
  }

  @Override
  protected void updateGuiModel() {

  }


  /**
   * Sets up the molecular formula generator for the neutralised ion mass. This includes the mass of
   * the adduct so the fragments can but don't have to include the adduct in their formula.
   */
  public MolecularFormulaGenerator setUpFormulaGenerator() {
//    final MolecularFormulaRange elementCounts = FragmentUtils.setupFormulaRange(assignedIonTypes);
    final double neutralMassWithAdduct =
        averageMZ * charge + polarity.getSign() * charge * FormulaUtils.electronMass;
    final Range<Double> formulaMassRange = formulaTolerance.getToleranceRange(
        neutralMassWithAdduct);

    final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    return new MolecularFormulaGenerator(builder, formulaMassRange.lowerEndpoint(),
        formulaMassRange.upperEndpoint(), elements);
  }

  public void generateFormulae(boolean couldBeRadical, MolecularFormulaGenerator generator) {
    IMolecularFormula formula = null;
    do {
      formula = generator.getNextFormula();
      if (isCanceled() || formulaCount >= maxFormulaCount) {
        break;
      }

      if (isFeasibleFormula(couldBeRadical, formula)) {
        formula.setCharge(polarity.getSign() * charge);

        final ResultFormula resultFormula = new ResultFormula(formula, measuredIsotopePattern,
            ms2Spectrum, averageMZ, ms2Tolerance, null);
//
        // i know we should not do this, but it will take forever otherwise.
        FxThread.runLater(() -> model.getPrecursorFormulae().add(resultFormula));
        formulaCount++;
      }
    } while (formula != null && formulaCount < DEFAULT_MAX_FORMULA_COUNT);
  }

  private boolean isFeasibleFormula(boolean couldBeRadical, IMolecularFormula formula) {
    if (formula == null) {
      return false;
    }

    if (!ElementalHeuristicChecker.checkFormula(formula, checkNOPS, checkNOPS, checkMultiple)) {
      return false;
    }
    final Double rdbe = RDBERestrictionChecker.calculateRDBE(formula);
    if (checkRDBE && rdbe != null) {
      if (rdbe - rdbe.intValue() > 0) {
        // we are looking at charged formulae (+h /-h) so valences will be .5 for formulae of integer rdbe.
        return true;
      } else if (Double.compare(rdbe - rdbe.intValue(), 0) == 0 && couldBeRadical) {
        return true;
      }
    }
    return false;
  }
}
