/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.parametertypes;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.parameters.UserParameter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

public class ClearAnnotationsParameter implements UserParameter<Map<DataType<?>, Boolean>, ClearAnnotationsComponent> {

  /**
   * List of all annotation types
   */
  private static final List<DataType<?>> annotationTypes;
  static {
    final List<DataType<?>> types = new ArrayList<>();
    try {
      ClassPath path = ClassPath.from(DataType.class.getClassLoader());
      path.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(info -> {
            try {
              Object o = info.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType && o instanceof AnnotationType) {
                types.add((DataType<?>) o);
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//               can go silent
//              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
    annotationTypes = Collections.unmodifiableList(types);
  }

  private final String name;
  private final String description;
  private Map<DataType<?>, Boolean> value = new LinkedHashMap<>();

  public ClearAnnotationsParameter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setValue(Map<DataType<?>, Boolean> newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return getValue() != null;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ClearAnnotationsComponent createEditingComponent() {
    return new ClearAnnotationsComponent(annotationTypes);
  }

  @Override
  public void setValueFromComponent(ClearAnnotationsComponent clearAnnotationsComponent) {
    value = clearAnnotationsComponent.getValue();
  }

  @Override
  public void setValueToComponent(ClearAnnotationsComponent clearAnnotationsComponent,
      Map<DataType<?>, Boolean> newValue) {
    clearAnnotationsComponent.setValue(newValue);
  }

  @Override
  public Map<DataType<?>, Boolean> getValue() {
    return value;
  }

  @Override
  public UserParameter<Map<DataType<?>, Boolean>, ClearAnnotationsComponent> cloneParameter() {
    var val = new LinkedHashMap<>(value);
    var param = new ClearAnnotationsParameter(name, description);
    param.setValue(val);
    return param;
  }
}
