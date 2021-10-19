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
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeMap;
import io.github.mzmine.datamodel.features.types.annotations.FormulaAnnotationType;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A modular annotation type displaying all subtypes for the first element in a list of {@link
 * IonIdentity} stored in {@link IonIdentityListType}
 */
public class IonIdentityModularType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List
      .of(new IonIdentityListType(), new IonNetworkIDType(), new SizeType(), new NeutralMassType(),
          new PartnerIdsType(), new MsMsMultimerVerifiedType(),
          // all formula types
          // list of IIN consensus formulas
          new FormulaConsensusSummaryType(),
          // List of formulas for this row and all related types
          new FormulaListType(), new FormulaMassType(), new RdbeType(),
          new MZType(), new MzPpmDifferenceType(), new MzAbsoluteDifferenceType(),
          new IsotopePatternScoreType(), new MsMsScoreType(), new CombinedScoreType());

  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Ion identity";
  }

  /**
   * On change of the first list element, change all the other sub types.
   *
   * @param data data property
   * @param ion  the new preferred ion (first element)
   */
  private void setCurrentElement(@NotNull ModularTypeMap data, @Nullable IonIdentity ion) {
    if (ion == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof IonIdentityListType)) {
          data.set(type, null);
        }
      }
    } else {
      // update selected values
      if (ion.getNetwork() != null) {
        data.set(FormulaConsensusSummaryType.class, ion.getNetwork().getMolFormulas());
        data.set(NeutralMassType.class, ion.getNetwork().getNeutralMass());
        data.set(IonNetworkIDType.class, ion.getNetwork().getID());
        data.set(SizeType.class, ion.getNetwork().size());
      } else {
        data.set(FormulaConsensusSummaryType.class, null);
        data.set(NeutralMassType.class, null);
        data.set(IonNetworkIDType.class, null);
        data.set(SizeType.class, null);
      }
      data.set(PartnerIdsType.class, ion.getPartnerRowsString(";"));
      data.set(MsMsMultimerVerifiedType.class, ion.getMSMSMultimerCount() > 0);

      // set all formulas and update the shown "best" formula
      data.set(FormulaListType.class, ion.getMolFormulas());
      setCurrentFormula(data, ion.getBestMolFormula());
    }
  }

  /**
   *
   */
  private void setCurrentFormula(@NotNull ModularTypeMap data,
      @Nullable ResultFormula formula) {
    // do not override all field if formula is none
    FormulaAnnotationType.setCurrentElement(data, formula, false);
  }


  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ion_identity_modular_type";
  }
}
