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

package io.github.mzmine.util.annotations;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * @param typeToCalculate The type to calculate the value for.
 * @param calc            A function to calculate the desired value of the type. May trhow an
 *                        {@link NullPointerException} (caught) but no other exception.
 * @param <T>
 */
public record ConnectedTypeCalculation<T>(@NotNull DataType<T> typeToCalculate,
                                          BiFunction<FeatureListRow, CompoundDBAnnotation, T> calc) {

  public static final List<ConnectedTypeCalculation<?>> LIST = List.of(

      new ConnectedTypeCalculation<>(DataTypes.get(FormulaType.class), (row, db) -> {
        final MolecularStructure structure = db.getStructure();
        if(structure != null) {
          return MolecularFormulaManipulator.getString(structure.formula());
        }
        return null;
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(PrecursorMZType.class), (row, db) -> {
        final IonType adduct = db.getAdductType(); // adduct defined
        final String formula = db.getFormula(); // formula calculated above
        final IMolecularFormula molFormula = FormulaUtils.createMajorIsotopeMolFormula(formula);
        try {
          final IMolecularFormula ionized = adduct.addToFormula(molFormula);
          return FormulaUtils.calculateMzRatio(ionized);
        } catch (CloneNotSupportedException e) {
          return null;
        }
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(MzPpmDifferenceType.class), (row, db) -> {
        final Double exactMass = db.get(PrecursorMZType.class);
        return (float) MathUtils.getPpmDiff(exactMass, row.getAverageMZ());
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(MzAbsoluteDifferenceType.class), (row, db) -> {
        final Double exactMass = db.get(PrecursorMZType.class);
        return MzAbsoluteDifferenceType.calculate(exactMass, row.getAverageMZ());
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(CCSRelativeErrorType.class), (row, db) -> {
        final Float ccs = db.get(CCSType.class);
        return PercentTolerance.getPercentError(ccs, row.getAverageCCS());
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(RtAbsoluteDifferenceType.class), (row, db) -> {
        final Float rt = db.get(RTType.class);
        return rt - row.getAverageRT();
      }),

      new ConnectedTypeCalculation<>(DataTypes.get(NeutralMassType.class),
          (row, db) -> CompoundDBAnnotation.calcNeutralMass(db)),

      new ConnectedTypeCalculation<>(DataTypes.get(IonTypeType.class), (row, db) -> {
        final Double neutralMass = db.get(NeutralMassType.class);
        var mod = IonModification.getBestIonModification(neutralMass, row.getAverageMZ(),
            SpectraMerging.defaultMs1MergeTol,
            row.getBestFeature().getRepresentativeScan().getPolarity());
        return new IonType(mod);
      }));

  public static final Map<DataType<?>, ConnectedTypeCalculation<?>> MAP = LIST.stream()
      .collect(Collectors.toMap(ConnectedTypeCalculation::typeToCalculate, ctc -> ctc));
  private static final Logger logger = Logger.getLogger(ConnectedTypeCalculation.class.getName());

  /**
   * Calculates and sets the value for the {@link #typeToCalculate()} if the value is not null.
   * Catches {@link NullPointerException} if it occurs in {@link #calc()}
   */
  public void calculateIfAbsent(FeatureListRow row, CompoundDBAnnotation annotation) {
    try {
      if (annotation.get(typeToCalculate) == null) {
        final T result = calc.apply(row, annotation);
        annotation.putIfNotNull(typeToCalculate, result);
      }
    } catch (NullPointerException e) {
      logger.info("Cannot calculate value for type: " + typeToCalculate.getUniqueID());
    }
  }
}
