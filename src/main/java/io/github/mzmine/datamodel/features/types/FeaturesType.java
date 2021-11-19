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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeaturesType extends DataType<Map<RawDataFile, ModularFeature>>
    implements NoTextColumn {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "features_map";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Features";
  }

  @Override
  public Property<Map<RawDataFile, ModularFeature>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<Map<RawDataFile, ModularFeature>> getValueClass() {
    return (Class) Map.class;
  }

}
