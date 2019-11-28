/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.Collections;
import java.util.Map;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.AreaType;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.HeightType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;

/**
 * Map of all feature related data.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ModularFeatureListRow implements ModularDataModel {

  private final DataTypeMap map = new DataTypeMap();

  @Override
  public DataTypeMap getMap() {
    return map;
  }

  // Helper methods
  // most common data types
  public FeatureStatus getDetectionType() {
    return get(DetectionType.class).map(DataType::getValue).orElse(FeatureStatus.UNKNOWN);
  }

  public Double getMZ() {
    return get(MZType.class).map(DataType::getValue).orElse(0d);
  }

  public Float getRT() {
    return get(RTType.class).map(DataType::getValue).orElse(0f);
  }

  public double getHeight() {
    return get(HeightType.class).map(DataType::getValue).orElse(0f);
  }

  public double getArea() {
    return get(AreaType.class).map(DataType::getValue).orElse(0f);
  }

  public Map<RawDataFile, ModularFeature> getFeatures() {
    return get(FeaturesType.class).map(DataType::getValue).orElse(Collections.emptyMap());
  }

}
