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

package io.github.mzmine.parameters;

import java.util.Arrays;
import java.util.Collection;
import org.w3c.dom.Element;

/**
 * Parameter interface, represents parameters or variables used in the project
 */
public interface Parameter<ValueType> {

  /**
   * Returns this parameter's name. The name must be unique within one ParameterSet.
   *
   * @return Parameter name
   */
  public String getName();

  public ValueType getValue();

  public void setValue(ValueType newValue);

  public boolean checkValue(Collection<String> errorMessages);

  public void loadValueFromXML(Element xmlElement);

  public void saveValueToXML(Element xmlElement);

  /**
   * We use isSensitive() to decide whether a parameter has to be encrypted (e.g. passwords).
   * Further, sensitive parameters are not written to project level configs
   */
  public default boolean isSensitive() {
    return false;
  }

  /**
   * We use cloneParameter() instead of clone() to force the implementing classes to implement this
   * method. Plain clone() is automatically implemented by the Object class.
   */
  public Parameter<ValueType> cloneParameter();

  public default boolean valueEquals(Parameter<?> that) {
    if(that == null) {
      return false;
    }
    if (!(this.getClass() == that.getClass())) {
      return false;
    }
    if (getValue() == null && that.getValue() == null) {
      return true;
    }
    if (getValue() instanceof Double) {
      return Double.compare((Double) this.getValue(), (Double) that.getValue()) == 0;
    } else if (getValue() instanceof Float) {
      return Float.compare((Float) this.getValue(), (Float) that.getValue()) == 0;
    } else if (getValue() instanceof Integer) {
      return ((Integer) getValue()).equals((Integer) that.getValue());
    } else if (getValue() instanceof String) {
      return this.getValue().equals(that.getValue());
    } else if (getValue() instanceof Object[]) {
      return Arrays.deepEquals((Object[]) getValue(), (Object[]) that.getValue());
    }
    return this.getValue().equals(that.getValue());
  }
}
