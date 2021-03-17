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
import io.github.mzmine.datamodel.features.types.numbers.IonNetworkIDType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.SizeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;

public class IonIdentityModularType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List
      .of(new IonIdentityListType(), new IonNetworkIDType(), new SizeType(), new NeutralMassType(),
          new PartnerIdsType(), new MsMsMultimerVerifiedType(), new FormulaAnnotationType());

  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Ion identities";
  }

  @Override
  public ModularTypeProperty createProperty() {
    final ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on this object
    property.get(IonIdentityListType.class)
        .addListener((ListChangeListener<IonIdentity>) change -> {
          ObservableList<? extends IonIdentity> summaryProperty = change.getList();
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
   * @param data data property
   * @param ion  the new preferred ion (first element)
   */
  private void setCurrentElement(ModularTypeProperty data, IonIdentity ion) {
    if (ion == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof SpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    } else {
      // update selected values
      if (ion.getBestMolFormula() != null) {
        data.get(FormulaAnnotationType.class)
            .set(FormulaAnnotationSummaryType.class, ion.getMolFormulas());
      }
      data.set(NeutralMassType.class, ion.getNetwork().getNeutralMass());
      data.set(IonNetworkIDType.class, ion.getNetwork().getID());
      data.set(SizeType.class, ion.getNetwork().size());
      data.set(PartnerIdsType.class, ion.getPartnerRows(";"));
      data.set(MsMsMultimerVerifiedType.class, ion.getMSMSMultimerCount() > 0);
    }
  }

}
