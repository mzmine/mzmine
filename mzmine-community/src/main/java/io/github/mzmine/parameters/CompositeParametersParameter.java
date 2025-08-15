/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import java.util.Map;
import javafx.scene.Node;
import org.w3c.dom.Element;

/**
 * Parameter that combines multiple parameters that are only internally used to handle sub values,
 * create components, save, load values...
 */
public abstract class CompositeParametersParameter<ValueType, EditorComponent extends Node> extends
    AbstractParameter<ValueType, EditorComponent> {


  public CompositeParametersParameter(String name, String description) {
    this(name, description, null);
  }

  public CompositeParametersParameter(String name, String description, ValueType defaultVal) {
    super(name, description, defaultVal);
  }

  protected abstract Parameter<?>[] getInternalParameters();

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final Map<String, Parameter<?>> nameParameterMap = ParameterUtils.getNameParameterMap(
        getInternalParameters());

    final Map<String, Parameter<?>> loadedParameters = ParameterUtils.loadValuesFromXML(
        this.getClass(), xmlElement, nameParameterMap);
    // currently this does nothing but just in case loaded parameters need to be handled
    // e.g. after changes to structure
    handleLoadedParameters(loadedParameters);
  }

  /**
   * Handle the loaded parameters. e.g., after change of parameters
   *
   * @param loadedParameters the actually loaded parameters
   */
  protected void handleLoadedParameters(Map<String, Parameter<?>> loadedParameters) {
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    ParameterUtils.saveValuesToXML(xmlElement, true, getInternalParameters());
  }

}
