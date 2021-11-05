/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.FormulaConsensusSummaryType;
import io.github.mzmine.datamodel.features.types.annotations.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.FormulaMassType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MsMsScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.SizeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A modular annotation type displaying all subtypes for the first element in a list of {@link
 * IonIdentity} stored in {@link SimpleIonIdentityListType}
 */
public class IonIdentityListType extends ListWithSubsType<IonIdentity> implements AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List
      .of(new IonNetworkIDType(), // start with netID
          new IonIdentityListType(), // add self type to have a column
          new SizeType(), new NeutralMassType(),
          new PartnerIdsType(), new MsMsMultimerVerifiedType(),
          // all formula types
          // list of IIN consensus formulas
          new FormulaConsensusSummaryType(),
          // List of formulas for this row and all related types
          new FormulaListType(), new FormulaMassType(), new RdbeType(),
          new MZType(), new MzPpmDifferenceType(), new MzAbsoluteDifferenceType(),
          new IsotopePatternScoreType(), new MsMsScoreType(), new CombinedScoreType());

  private static final Map<Class<? extends DataType>, Function<IonIdentity, Object>> mapper =
      Map.ofEntries(
          createEntry(IonIdentityListType.class, (ion -> ion)),
          createEntry(IonNetworkIDType.class,
              (ion -> ion.getNetwork() != null ? ion.getNetID() : null)),
          createEntry(SizeType.class,
              (ion -> ion.getNetwork() != null ? ion.getNetwork().size() : null)),
          createEntry(NeutralMassType.class,
              (ion -> ion.getNetwork() != null ? ion.getNetwork().getNeutralMass() : null)),
          createEntry(PartnerIdsType.class, (ion -> ion.getPartnerRowsString(";"))),
          createEntry(MsMsMultimerVerifiedType.class, (ion -> {
            int msmsMultimerCount = ion.getMSMSMultimerCount();
            return msmsMultimerCount == -1 ? null : msmsMultimerCount > 0;
          })),
          createEntry(FormulaConsensusSummaryType.class,
              (ion -> ion.getNetwork() != null ? ion.getNetwork().getMolFormulas() : null)),
          createEntry(FormulaListType.class, (ion -> ion.getMolFormulas())),
          createEntry(FormulaMassType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : f.getExactMass();
          })),
          createEntry(RdbeType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : f.getRDBE();
          })),
          createEntry(MZType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : ion.getIonType().getMZ(f.getExactMass());
          })),
          createEntry(MzPpmDifferenceType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : f.getPpmDiff();
          })),
          createEntry(MzAbsoluteDifferenceType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : f.getAbsoluteMzDiff();
          })),
          createEntry(IsotopePatternScoreType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null || f.getIsotopeScore() == null ? null : f.getIsotopeScore();
          })),
          createEntry(MsMsScoreType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null || f.getMSMSScore() == null ? null : f.getMSMSScore();
          })),
          createEntry(CombinedScoreType.class, (ion -> {
            ResultFormula f = getMolFormula(ion);
            return f == null ? null : f.getScore(10, 3, 1);
          }))
      );

  private static @Nullable ResultFormula getMolFormula(@NotNull IonIdentity ion) {
    List<ResultFormula> formulas = ion.getMolFormulas();
    return formulas == null || formulas.isEmpty() ? null : formulas.get(0);
  }

  @Override
  public List<DataType> getSubDataTypes() {
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
    return "ion_identity_list";
  }
}
