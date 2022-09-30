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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.XICManualPickerModule;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AreaType extends HeightType {

  @NotNull
  @Override
  public String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "area";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Area";
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.MAX));
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, @Nullable DataType<?> superType, @Nullable Object value) {

    if(file.size() == 1) {
      final ModularFeature selectedFeature = row.getFeature(file.get(0));

      if(selectedFeature != null && selectedFeature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        return () -> MZmineCore.runLater(() -> XICManualPickerModule.runManualDetection(selectedFeature.getRawDataFile(),
            row, row.getFeatureList()));
      }
    }

    return null;
  }
}
