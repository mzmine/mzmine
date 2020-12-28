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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;
import javax.annotation.Nonnull;

public class ChargeType extends IntegerType {

  @Override
  public String getHeaderString() {
    return "Charge";
  }


  @Nonnull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked")
    Property<Integer>[] prop = row.streamFeatures().map(f -> (ModularFeature) f)
        .map(f -> f.get(this)).toArray(Property[]::new);
    switch (bind) {
      case CONSENSUS:
        return Bindings.createObjectBinding(() -> {
          Map<Integer, Integer> count = new HashMap<>();
          for (Property<Integer> p : prop) {
            if (p.getValue() != null) {
              Integer charge = p.getValue();
              Integer n = count.get(charge);
              count.put(charge, n==null? 1 : n+1);
            }
          }
          return count.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getValue)
                  .orElse(0);
        }, prop);
    }
    return super.createBinding(bind, row);
  }
}
