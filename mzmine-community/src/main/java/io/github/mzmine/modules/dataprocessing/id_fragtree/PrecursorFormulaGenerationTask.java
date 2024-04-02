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

package io.github.mzmine.modules.dataprocessing.id_fragtree;

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

public class PrecursorFormulaGenerationTask implements TaskSubProcessor,
    TaskSubSupplier<ConcurrentLinkedQueue<IMolecularFormula>> {

  private Task parent;
  private FeatureListRow row;
  private IonType ionTypeOverride;
  private MZTolerance formulaTolerance;
  private boolean checkCHONPS;
  private boolean checkRDBE;
  final ConcurrentLinkedQueue<IMolecularFormula> formulae = new ConcurrentLinkedQueue<>();
  private MolecularFormulaGenerator generator;

  public PrecursorFormulaGenerationTask(FragmentTreeCalcTask parent, FeatureListRow row,
      IonType ionTypeOverride, MZTolerance formulaTolerance, boolean checkCHONPS,
      boolean checkRDBE) {

    this.parent = parent;
    this.row = row;
    this.ionTypeOverride = ionTypeOverride;
    this.formulaTolerance = formulaTolerance;
    this.checkCHONPS = checkCHONPS;
    this.checkRDBE = checkRDBE;
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
    return STR."Generating precursor formulae for \{row.toString()}";
  }

  @Override
  public double getFinishedPercentage() {
    return generator == null ? 0 : generator.getFinishedPercentage();
  }

  @Override
  public void process() {
    final PolarityType polarity = row.getBestFeature().getRepresentativeScan().getPolarity();
    final List<IonType> assignedIonTypes;
    if (ionTypeOverride != null) {
      assignedIonTypes = List.of(ionTypeOverride);
    } else {
      assignedIonTypes = FeatureUtils.extractAllIonTypes(row);
    }

    final MolecularFormulaRange elementCounts = FragTreeUtils.setupFormulaRange(assignedIonTypes);
    final boolean couldBeRadical = assignedIonTypes.stream()
        .anyMatch(ion -> ion.getAdduct().isElectron());

    final double neutralMassWithAdduct = row.getAverageMZ() * row.getRowCharge()
        + polarity.getSign() * row.getRowCharge() * FormulaUtils.electronMass;
    final Range<Double> formulaMassRange = formulaTolerance.getToleranceRange(
        neutralMassWithAdduct);

    final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    generator = new MolecularFormulaGenerator(builder, formulaMassRange.lowerEndpoint(),
        formulaMassRange.upperEndpoint(), elementCounts);

    IMolecularFormula formula = null;

    do {
      formula = generator.getNextFormula();
      if (formula != null && //
          (checkCHONPS && ElementalHeuristicChecker.checkFormula(formula, true, true, true))) {

        final Double rdbe = RDBERestrictionChecker.calculateRDBE(formula);
        if (checkRDBE && rdbe != null) {
          if (rdbe - rdbe.intValue() > 0) {
            formulae.add(formula);
          } else if (Double.compare(rdbe - rdbe.intValue(), 0) == 0 && couldBeRadical) {
            formulae.add(formula);
          }
        }
      }
    } while (formula != null);
  }

  @Override
  public ConcurrentLinkedQueue<IMolecularFormula> get() {
    return formulae;
  }
}
