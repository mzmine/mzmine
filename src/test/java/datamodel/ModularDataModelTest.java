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

package datamodel;

import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.reflect.MutableTypeToInstanceMap;
import com.google.common.reflect.TypeToInstanceMap;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import javafx.beans.property.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@ExtendWith(MockitoExtension.class)
public class ModularDataModelTest {
  @Mock
  ModularFeatureList flist;
  
  @Test
  public void testModularDataModel() {
    ModularFeature feature = new ModularFeature(flist);
    DataType<Double> typeColumn = feature.getTypeColumn(MZType.class);
    feature.get(MZType.class);

  }
}
