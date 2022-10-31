/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A modular annotation type displaying all subtypes for the first element in a list of {@link
 * IonIdentity} stored in {@link SimpleIonIdentityListType}
 */
public class IonIdentityListType extends ListWithSubsType<IonIdentity> implements AnnotationType {

  private static final Logger logger = Logger.getLogger(IonIdentityListType.class.getName());
  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new IonNetworkIDType(),
      // start with netID
      new IonIdentityListType(), // add self type to have a column
      new SizeType(), new NeutralMassType(), new PartnerIdsType(), new MsMsMultimerVerifiedType(),
      // all realtionship types
      new IINRelationshipsType(), new IINRelationshipsSummaryType(),
      // all formula types
      // list of IIN consensus formulas
      new ConsensusFormulaListType(),
      // List of formulas for this row and all related types
      new SimpleFormulaListType(), new FormulaMassType(), new RdbeType(), new MZType(),
      new MzPpmDifferenceType(), new MzAbsoluteDifferenceType(), new IsotopePatternScoreType(),
      new MsMsScoreType(), new CombinedScoreType());

  private static final Map<Class<? extends DataType>, Function<IonIdentity, Object>> mapper = Map.ofEntries(
      createEntry(IonIdentityListType.class, (ion -> ion)), createEntry(IonNetworkIDType.class,
          (ion -> ion.getNetwork() != null ? ion.getNetID() : null)), createEntry(SizeType.class,
          (ion -> ion.getNetwork() != null ? ion.getNetwork().size() : null)),
      createEntry(NeutralMassType.class,
          (ion -> ion.getNetwork() != null ? ion.getNetwork().getNeutralMass() : null)),
      createEntry(PartnerIdsType.class, (ion -> ion.getPartnerRowsString(";"))),
      createEntry(MsMsMultimerVerifiedType.class, (ion -> {
        int msmsMultimerCount = ion.getMSMSMultimerCount();
        return msmsMultimerCount == -1 ? null : msmsMultimerCount > 0;
      })),
      // list of relationships has no order
      createEntry(IINRelationshipsType.class, (ion -> ion.getNetwork() != null ? new ArrayList<>(
          ion.getNetwork().getRelations().entrySet()) : null)),
      createEntry(IINRelationshipsSummaryType.class,
          (ion -> ion.getNetwork() != null && ion.getNetwork().getRelations() != null
              ? ion.getNetwork().getRelations().entrySet().stream()
              .map(entry -> entry.getValue().getName(entry.getKey()))
              .collect(Collectors.joining(";")) : null)),
      //
      createEntry(ConsensusFormulaListType.class,
          (ion -> ion.getNetwork() != null ? ion.getNetwork().getMolFormulas() : null)),
      createEntry(SimpleFormulaListType.class, (ion -> ion.getMolFormulas())),
      createEntry(FormulaMassType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : f.getExactMass();
      })), createEntry(RdbeType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : f.getRDBE();
      })), createEntry(MZType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : ion.getIonType().getMZ(f.getExactMass());
      })), createEntry(MzPpmDifferenceType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : f.getPpmDiff();
      })), createEntry(MzAbsoluteDifferenceType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : f.getAbsoluteMzDiff();
      })), createEntry(IsotopePatternScoreType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null || f.getIsotopeScore() == null ? null : f.getIsotopeScore();
      })), createEntry(MsMsScoreType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null || f.getMSMSScore() == null ? null : f.getMSMSScore();
      })), createEntry(CombinedScoreType.class, (ion -> {
        ResultFormula f = getMolFormula(ion);
        return f == null ? null : f.getScore(10, 3, 1);
      })));

  private static @Nullable ResultFormula getMolFormula(@NotNull IonIdentity ion) {
    List<ResultFormula> formulas = ion.getMolFormulas();
    return formulas == null || formulas.isEmpty() ? null : formulas.get(0);
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public Map<Class<? extends DataType>, Function<IonIdentity, Object>> getMapper() {
    return mapper;
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
