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

import java.util.Collection;
import org.w3c.dom.Element;

/**
 * Wraps a Parameter to hide its component in the setup dialog
 *
 * @param <V> value type of the parameter
 */
public class HiddenParameter<V> implements Parameter<V> {

  private final Parameter<V> embeddedParameter;

  public HiddenParameter(Parameter<V> embeddedParameter) {
    this.embeddedParameter = embeddedParameter;
  }

  @Override
  public String getName() {
    return embeddedParameter.getName();
  }

  @Override
  public V getValue() {
    return embeddedParameter.getValue();
  }

  @Override
  public void setValue(final V newValue) {
    embeddedParameter.setValue(newValue);
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    return embeddedParameter.checkValue(errorMessages);
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    embeddedParameter.saveValueToXML(xmlElement);
  }

  @Override
  public boolean isSensitive() {
    return embeddedParameter.isSensitive();
  }

  @Override
  public Parameter<V> cloneParameter() {
    return new HiddenParameter<>(embeddedParameter.cloneParameter());
  }

  @Override
  public boolean valueEquals(final Parameter<?> that) {
    if (that instanceof HiddenParameter<?> hidden) {
      return embeddedParameter.valueEquals(hidden.embeddedParameter);
    }
    return embeddedParameter.valueEquals(that);
  }

  public Parameter<V> getEmbeddedParameter() {
    return embeddedParameter;
  }
}
