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

import java.util.Collection;
import org.w3c.dom.Element;

import io.github.mzmine.parameters.Parameter;

/**
 * This is a container for any user parameter, that is not shown in the parameter setup dialog.
 * HiddenParameter can be used to store additional variables, that are not shown in the parameter
 * setup dialog. E.g.: spectraProcessing in
 * {@link io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingParameters#spectraProcessing
 * this} is a HiddenParameter, so it can be toggled by a checkbox in a different window.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 * @param <ParameterType> The type of the contained UserParameter
 * @param <ValueType> The value type of the contained UserParameter
 * 
 * @see UserParameter
 */
public class HiddenParameter<ParameterType extends Parameter<?>, ValueType>
    implements Parameter<ValueType> {

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
  public boolean checkValue(Collection errorMessages) {
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
  public HiddenParameter<ParameterType, ValueType> cloneParameter() {
    Parameter<ValueType> param = getEmbeddedParameter().cloneParameter();
    return new HiddenParameter<ParameterType, ValueType>(param);
  }

  public Parameter<ValueType> getEmbeddedParameter() {
    return embeddedParameter;
  }

  private void setEmbeddedParameter(Parameter<ValueType> param) {
    this.embeddedParameter = param;
  }
}
