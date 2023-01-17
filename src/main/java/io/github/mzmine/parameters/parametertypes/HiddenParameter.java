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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.w3c.dom.Element;

/**
 * This is a container for any user parameter, that is not shown in the parameter setup dialog.
 * HiddenParameter can be used to store additional variables, that are not shown in the parameter
 * setup dialog.
 *
 * @param <ValueType> The value type of the contained UserParameter
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 * @see UserParameter
 */
public class HiddenParameter<ValueType> implements Parameter<ValueType> {

  private Parameter<ValueType> embeddedParameter;

  public HiddenParameter(Parameter<ValueType> param) {
    setEmbeddedParameter(param);
  }

  @Override
  public String getName() {
    return getEmbeddedParameter().getName();
  }

  @Override
  public ValueType getValue() {
    return getEmbeddedParameter().getValue();
  }

  @Override
  public void setValue(ValueType newValue) {
    getEmbeddedParameter().setValue(newValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return getEmbeddedParameter().checkValue(errorMessages);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    getEmbeddedParameter().loadValueFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    getEmbeddedParameter().saveValueToXML(xmlElement);
  }

  @Override
  public HiddenParameter<ValueType> cloneParameter() {
    return new HiddenParameter<>(embeddedParameter.cloneParameter());
  }

  public Parameter<ValueType> getEmbeddedParameter() {
    return embeddedParameter;
  }

  private void setEmbeddedParameter(Parameter<ValueType> param) {
    this.embeddedParameter = param;
  }

  @Override
  public boolean valueEquals(final Parameter<?> that) {
    if (that instanceof HiddenParameter<?> hidden) {
      return embeddedParameter.valueEquals(hidden.embeddedParameter);
    }
    return embeddedParameter.valueEquals(that);
  }

  @Override
  public boolean isSensitive() {
    return embeddedParameter.isSensitive();
  }
}
