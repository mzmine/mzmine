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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.ListRowBinding;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Holds all data points of a {@link ModularFeature}. Change listener from {@link
 * #createDefaultRowBindings()} will create and update the {@link FeatureShapeType}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class DataPointsType extends ListDataType<DataPoint> implements NullColumnType {

  @Override
  public String getHeaderString() {
    return "DPs";
  }

  @Nonnull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    // listen to changes in DataPointsType for all ModularFeatures
    return List.of(new ListRowBinding(new FeatureShapeType(), this));
  }

}
