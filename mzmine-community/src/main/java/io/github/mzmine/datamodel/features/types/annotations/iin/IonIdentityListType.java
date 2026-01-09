/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.annotations.formula.ConsensusFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaMassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.SimpleFormulaListType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.SizeType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A modular annotation type displaying all subtypes for the first element in a list of
 * {@link IonIdentity} stored in {@link SimpleIonIdentityListType}
 */
public class IonIdentityListType extends ListWithSubsType<IonIdentity> implements AnnotationType {

  private static final Logger logger = Logger.getLogger(IonIdentityListType.class.getName());
  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new IonNetworkIDType(),
      // start with netID
      new IonIdentityListType(), // add self type to have a column
      new SizeType(), new NeutralMassType(),
      // all realtionship types
      new IINRelationshipsType(), new IINRelationshipsSummaryType(),
      // all formula types
      // list of IIN consensus formulas
      new ConsensusFormulaListType(),
      // List of formulas for this row and all related types
      new SimpleFormulaListType(), new FormulaMassType(), new RdbeType(), new MZType(),
      new MzPpmDifferenceType(), new MzAbsoluteDifferenceType(), new IsotopePatternScoreType(),
      new MsMsScoreType(), new CombinedScoreType());

  private static @Nullable ResultFormula getMolFormula(@NotNull IonIdentity ion) {
    List<ResultFormula> formulas = ion.getMolFormulas();
    return formulas.isEmpty() ? null : formulas.get(0);
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public <K> @Nullable K map(@NotNull final DataType<K> subType, final IonIdentity ion) {
    final IonNetwork net = ion.getNetwork();
    return (K) switch (subType) {
      case IonIdentityListType __ -> ion;
      case IonNetworkIDType __ -> net != null ? ion.getNetID() : null;
      case SizeType __ -> net != null ? net.size() : null;
      case NeutralMassType __ -> net != null ? net.getNeutralMass() : null;
      // list of relationships has no order
      case IINRelationshipsType __ ->
          net != null ? new ArrayList<>(net.getRelations().entrySet()) : null;
      case IINRelationshipsSummaryType __ ->
          net != null && net.getRelations() != null ? net.getRelations().entrySet().stream()
              .map(entry -> entry.getValue().getName(entry.getKey()))
              .collect(Collectors.joining(";")) : null;
      //
      case ConsensusFormulaListType __ -> net != null ? net.getMolFormulas() : null;
      case SimpleFormulaListType __ -> ion.getMolFormulas();
      case FormulaMassType __ ->
          ion.getBestMolFormula().map(MolecularFormulaIdentity::getExactMass).orElse(null);
      case RdbeType __ ->
          ion.getBestMolFormula().map(MolecularFormulaIdentity::getRDBE).orElse(null);
      case MZType __ ->
          ion.getBestMolFormula().map(MolecularFormulaIdentity::getExactMass).orElse(null);
      case MzPpmDifferenceType __ ->
          ion.getBestMolFormula().map(ResultFormula::getPpmDiff).orElse(null);
      case MzAbsoluteDifferenceType __ ->
          ion.getBestMolFormula().map(ResultFormula::getAbsoluteMzDiff).orElse(null);
      case IsotopePatternScoreType __ ->
          ion.getBestMolFormula().map(ResultFormula::getIsotopeScore).orElse(null);
      case MsMsScoreType __ ->
          ion.getBestMolFormula().map(ResultFormula::getMSMSScore).orElse(null);
      case CombinedScoreType __ ->
          ion.getBestMolFormula().map(f -> f.getScore(10, 3, 1)).orElse(null);
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map".formatted(subType.toString()));
    };
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Ion identity";
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ion_identities";
  }

  @Override
  public <T> void valueChanged(ModularDataModel model, DataType<T> subType, int subColumnIndex,
      T newValue) {
    try {
      if (subType.getClass().equals(ConsensusFormulaListType.class)) {
        List<ResultFormula> formulas = model.get(this).get(0).getNetwork().getMolFormulas();
        formulas.remove(newValue);
        formulas.add(0, (ResultFormula) newValue);
      } else if (subType.getClass().equals(SimpleFormulaListType.class)) {
        List<ResultFormula> formulas = model.get(this).get(0).getMolFormulas();
        formulas.remove(newValue);
        formulas.add(0, (ResultFormula) newValue);
      } else if (subType.getClass().equals(IonIdentityListType.class)) {
        List<IonIdentity> ions = model.get(this);
        if (ions != null) {
          ions = new ArrayList<>(ions);
          ions.remove(newValue);
          ions.add(0, (IonIdentity) newValue);
          model.set(this, ions);
        }
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, () -> String.format(
          "Cannot handle change in subtype %s at index %d in parent type %s with new value %s",
          subType.getClass().getName(), subColumnIndex, this.getClass().getName(), newValue));
    }
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
