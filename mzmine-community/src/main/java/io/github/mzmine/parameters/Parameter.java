/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
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
  String getName();

  ValueType getValue();

  void setValue(ValueType newValue);

  boolean checkValue(Collection<String> errorMessages);

  void loadValueFromXML(Element xmlElement);

  void saveValueToXML(Element xmlElement);

  /**
   * We use isSensitive() to decide whether a parameter has to be encrypted (e.g. passwords).
   * Further, sensitive parameters are not written to project level configs
   */
  default boolean isSensitive() {
    return false;
  }

  /**
   * We use cloneParameter() instead of clone() to force the implementing classes to implement this
   * method. Plain clone() is automatically implemented by the Object class.
   */
  Parameter<ValueType> cloneParameter();

  default boolean valueEquals(Parameter<?> that) {
    if (that == null) {
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
    } else if (getValue() instanceof Object[]) {
      return Arrays.deepEquals((Object[]) getValue(), (Object[]) that.getValue());
    }
    return Objects.equals(this.getValue(), that.getValue());
  }
}
