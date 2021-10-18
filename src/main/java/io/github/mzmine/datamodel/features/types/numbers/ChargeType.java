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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This type describes the negative or positive charge of an ion (feature)
 */
public class ChargeType extends IntegerType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "charge";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Charge";
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  /**
   * The consensus charge will be the lowest charge with the maximum count in the feature list row
   *
   * @param bindingType
   * @param models
   * @return
   */
  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    switch (bindingType) {
      case CONSENSUS: {
        Map<Integer, Integer> max = new HashMap<>();
        for (ModularDataModel model : models) {
          if (model != null) {
            Integer charge = model.get(this);
            max.merge(charge, 1, Integer::sum);
          }
        }
        Integer maxCharge = 0;
        Integer maxCount = 0;
        for (var entry : max.entrySet()) {
          if (entry.getValue() > maxCount) {
            maxCount = entry.getValue();
            maxCharge = entry.getKey();
          }
        }
        return maxCharge;
      }
      default:
        return super.evaluateBindings(bindingType, models);
    }
  }
}
