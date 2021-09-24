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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsFactoryType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanNumbersType extends ListDataType<Scan> implements BindingsFactoryType {

  @NotNull
  @Override
  public String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "scan_numbers";
  }

  @Override
  public String getHeaderString() {
    return "Scans";
  }

  @NotNull
  @Override
  public String getFormattedString(@NotNull ListProperty<Scan> property) {
    return property.getValue() != null ? String.valueOf(property.getValue().size()) : "";
  }

  @NotNull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if (value == null || !(value instanceof List)) {
      return "";
    }
    return String.valueOf(((List) value).size());
  }


  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked")
    ListProperty[] prop = row.streamFeatures().map(f -> (ModularFeature) f)
        .map(f -> f.get(this)).toArray(ListProperty[]::new);
    switch (bind) {
      case AVERAGE:
        return Bindings.createObjectBinding(() -> {
          float sum = 0;
          int n = 0;
          for (ListProperty p : prop) {
            if (p.getValue() != null) {
              sum += p.size();
              n++;
            }
          }
          return n == 0 ? 0 : sum / n;
        }, prop);
      case MIN:
        return Bindings.createObjectBinding(() -> {
          int min = Integer.MAX_VALUE;
          for (ListProperty p : prop) {
            if (p.getValue() != null && p.size() < min) {
              min = p.size();
            }
          }
          return min;
        }, prop);
      case MAX:
        return Bindings.createObjectBinding(() -> {
          int max = Integer.MIN_VALUE;
          for (ListProperty p : prop) {
            if (p.getValue() != null && p.size() > max) {
              max = p.size();
            }
          }
          return max;
        }, prop);
      case SUM:
        return Bindings.createObjectBinding(() -> {
          int sum = 0;
          for (ListProperty p : prop) {
            if (p.getValue() != null) {
              sum += p.size();
            }
          }
          return sum;
        }, prop);
      case CONSENSUS:
        return Bindings.createObjectBinding(() -> {
          List collect = (List) Stream.of(prop).filter(Objects::nonNull)
              .flatMap(ListProperty::stream).collect(Collectors.toList());
          return FXCollections.observableList(collect);
        }, prop);
      case COUNT:
      case RANGE:
      default:
        throw new UndefinedRowBindingException(this, bind);
    }
  }
}
