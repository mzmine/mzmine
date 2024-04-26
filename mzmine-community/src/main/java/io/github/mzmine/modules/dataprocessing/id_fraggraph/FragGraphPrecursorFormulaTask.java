/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_fraggraph;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.operations.TaskSubProcessor;
import io.github.mzmine.taskcontrol.operations.TaskSubSupplier;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class FragGraphPrecursorFormulaTask implements TaskSubProcessor,
    TaskSubSupplier<ConcurrentLinkedQueue<IMolecularFormula>> {

  private static final int DEFAULT_MAX_FORMULA_COUNT = 100;

  private Task parent;
  private final IonType ionTypeOverride;
  private final MZTolerance formulaTolerance;
  private final boolean checkCHONPS;
  private final boolean checkRDBE;

  /**
   * List of viable formulae. This queue may be polled from another task to compute the
   * fragmentation tree.
   */
  final ConcurrentLinkedQueue<IMolecularFormula> formulae = new ConcurrentLinkedQueue<>();
  private MolecularFormulaGenerator generator;

  private final int maxFormulaCount;
  private int formulaCount = 0;
  private final Integer charge;
  private final PolarityType polarity;
  private final double averageMZ;
  private final String desc;
  private final List<IonType> assignedIonTypes;

  public FragGraphPrecursorFormulaTask(@Nullable Task parent, @NotNull FeatureListRow row,
      @Nullable IonType ionTypeOverride, @NotNull MZTolerance formulaTolerance, boolean checkCHONPS,
      boolean checkRDBE) {

    this(parent, row, ionTypeOverride, formulaTolerance, checkCHONPS, checkRDBE,
        DEFAULT_MAX_FORMULA_COUNT);
  }

  public FragGraphPrecursorFormulaTask(@Nullable Task parent, @NotNull FeatureListRow row,
      @Nullable IonType ionTypeOverride, @NotNull MZTolerance formulaTolerance, boolean checkCHONPS,
      boolean checkRDBE, int maxFormulaCount) {

    this.parent = parent;
    charge = row.getRowCharge();
    polarity = row.getBestFeature().getRepresentativeScan().getPolarity();
    averageMZ = row.getAverageMZ();
    this.desc = STR."Generating precursor formulae for \{row.toString()}";

    if (ionTypeOverride != null) {
      assignedIonTypes = List.of(ionTypeOverride);
    } else {
      assignedIonTypes = FeatureUtils.extractAllIonTypes(row);
    }

    this.ionTypeOverride = ionTypeOverride;
    this.formulaTolerance = formulaTolerance;
    this.checkCHONPS = checkCHONPS;
    this.checkRDBE = checkRDBE;
    this.maxFormulaCount = maxFormulaCount;
  }

  public FragGraphPrecursorFormulaTask(@Nullable Task parent, double mz, PolarityType polarity,
      int charge, @NotNull List<IonType> possibleIonTypes, @Nullable IonType ionTypeOverride,
      @NotNull MZTolerance formulaTolerance, boolean checkCHONPS, boolean checkRDBE,
      int maxFormulaCount) {

    this.parent = parent;
    this.charge = charge;
    this.polarity = polarity;
    this.averageMZ = mz;
    this.desc = STR."Generating precursor formulae for \{String.format("%.4f", mz)}";

    if (ionTypeOverride != null) {
      assignedIonTypes = List.of(ionTypeOverride);
    } else {
      assignedIonTypes = possibleIonTypes;
    }

    this.ionTypeOverride = ionTypeOverride;
    this.formulaTolerance = formulaTolerance;
    this.checkCHONPS = checkCHONPS;
    this.checkRDBE = checkRDBE;
    this.maxFormulaCount = maxFormulaCount;
  }

  @Override
  public Task getParentTask() {
    return parent;
  }

  @Override
  public void setParentTask(@Nullable Task parentTask) {
    this.parent = parentTask;
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
    final boolean couldBeRadical = assignedIonTypes.stream()
        .anyMatch(ion -> ion.getAdduct().isElectron());

    generator = setUpFormulaGenerator();
    generateFormulae(couldBeRadical, generator);
  }


  /**
   * Sets up the molecular formula generator for the neutralised ion mass. This includes the mass of
   * the adduct so the fragments can but don't have to include the adduct in their formula.
   */
  public MolecularFormulaGenerator setUpFormulaGenerator() {
    final MolecularFormulaRange elementCounts = FragmentUtils.setupFormulaRange(assignedIonTypes);
    final double neutralMassWithAdduct =
        averageMZ * charge + polarity.getSign() * charge * FormulaUtils.electronMass;
    final Range<Double> formulaMassRange = formulaTolerance.getToleranceRange(
        neutralMassWithAdduct);

    final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    return new MolecularFormulaGenerator(builder, formulaMassRange.lowerEndpoint(),
        formulaMassRange.upperEndpoint(), elementCounts);
  }

  public void generateFormulae(boolean couldBeRadical, MolecularFormulaGenerator generator) {
    IMolecularFormula formula = null;
    do {
      formula = generator.getNextFormula();
      if (isFeasibleFormula(couldBeRadical, formula)) {
        formula.setCharge(polarity.getSign() * charge);
        formulae.add(formula);
        formulaCount++;
      }
      if (isCanceled() || (parent != null && parent.isCanceled())
          || formulaCount >= maxFormulaCount) {
        break;
      }
    } while (formula != null);
  }

  private boolean isFeasibleFormula(boolean couldBeRadical, IMolecularFormula formula) {
    if (formula == null) {
      return false;
    }
    if (formula != null && //
        (checkCHONPS && ElementalHeuristicChecker.checkFormula(formula, true, true, true))) {

      final Double rdbe = RDBERestrictionChecker.calculateRDBE(formula);
      if (checkRDBE && rdbe != null) {
        if (rdbe - rdbe.intValue() > 0) {
          return true;
        } else if (Double.compare(rdbe - rdbe.intValue(), 0) == 0 && couldBeRadical) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public ConcurrentLinkedQueue<IMolecularFormula> get() {
    return formulae;
  }
}
