/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

public class ManualAnnotationType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List
      .of(new IdentityType(), new CommentType(), new CompoundNameType(), new IonAdductType(),
          new FormulaType(), new SmilesStructureType());

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Manual annotation";
  }

  @Override
  public ModularTypeProperty createProperty() {
    final ModularTypeProperty property = super.createProperty();

    property.get(IdentityType.class)
        .addListener((ListChangeListener<? super FeatureIdentity>) change -> {
          ObservableList<? extends FeatureIdentity> list = change.getList();
          boolean firstElementChanged = false;
          while (change.next()) {
            firstElementChanged = firstElementChanged || change.getFrom() == 0;
          }
          if (firstElementChanged) {
            // first list elements has changed - set all other fields
            setCurrentElement(property, list.isEmpty() ? null : list.get(0));
          }
        });
    return property;
  }

  private void setCurrentElement(ModularTypeProperty data, FeatureIdentity identity) {
    if (identity == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof IdentityType)) {
          data.set(type, null);
        }
      }
    } else {
      data.set(CommentType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_COMMENT));
      data.set(CompoundNameType.class, identity.getName());
      data.set(IonAdductType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_ADDUCT));
      data.set(FormulaType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA));
      data.set(SmilesStructureType.class,
          identity.getPropertyValue(FeatureIdentity.PROPERTY_SMILES));
    }
  }
}
