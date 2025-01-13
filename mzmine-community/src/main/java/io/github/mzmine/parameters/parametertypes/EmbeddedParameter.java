/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.scene.Node;

/**
 * embedds another parameter and uses the name and description
 */
public abstract class EmbeddedParameter<ValueType, PARAMETER extends UserParameter<?, ?>, EditorComponent extends Node> implements
    UserParameter<ValueType, EditorComponent>, ParameterContainer {

  protected PARAMETER embeddedParameter;

  public EmbeddedParameter(ValueType defaultVal, PARAMETER embeddedParameter) {
    // requires cloning to avoid usage of static parameters
    this.embeddedParameter = (PARAMETER) embeddedParameter.cloneParameter();
    setValue(defaultVal);
  }

  public PARAMETER getEmbeddedParameter() {
    return embeddedParameter;
  }

  public void setEmbeddedParameter(PARAMETER embeddedParameter) {
    this.embeddedParameter = embeddedParameter;
  }

  @Override
  public String getName() {
    return embeddedParameter.getName();
  }

  @Override
  public String getDescription() {
    return embeddedParameter.getDescription();
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return embeddedParameter.checkValue(errorMessages);
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    if (embeddedParameter instanceof ParameterContainer pc) {
      pc.setSkipSensitiveParameters(skipSensitiveParameters);
    }
  }

}
