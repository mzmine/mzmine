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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MsMsScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.RdbeType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.List;
import java.util.Objects;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;

public class FormulaAnnotationType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List
      .of(new FormulaAnnotationSummaryType(), new NeutralMassType(), new RdbeType(),
          new MZType(), new MzPpmDifferenceType(), new MzAbsoluteDifferenceType(),
          new IsotopePatternScoreType(), new MsMsScoreType(), new CombinedScoreType());

  @Nonnull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Formula";
  }

  @Override
  public ModularTypeProperty createProperty() {
    ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on this object
    Objects.requireNonNull(property.get(FormulaAnnotationSummaryType.class))
        .addListener((ListChangeListener<ResultFormula>) change -> {
          ObservableList<? extends ResultFormula> summaryProperty = change.getList();
          boolean firstElementChanged = false;
          while (change.next()) {
            firstElementChanged = firstElementChanged || change.getFrom() == 0;
          }
          if (firstElementChanged) {
            // first list elements has changed - set all other fields
            setCurrentElement(property, summaryProperty.isEmpty() ? null : summaryProperty.get(0));
          }
        });

    return property;
  }

  /**
   * On change of the first list element, change all the other sub types.
   *
   * @param data    property
   * @param formula the new preferred formula (first element)
   */
  private void setCurrentElement(ModularTypeProperty data, ResultFormula formula) {
    if (formula == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof SpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    } else {
      // update selected values
      data.set(NeutralMassType.class, formula.getExactMass());
      data.set(IsotopePatternScoreType.class, formula.getIsotopeScore());
      data.set(MsMsScoreType.class, formula.getMSMSScore());
      data.set(RdbeType.class, formula.getRDBE());
    }
  }

}
